package br.com.setis.desafiojava.service.impl;

import static br.com.setis.desafiojava.domain.entity.StatusTransacao.PERMITEM_REEMBOLSO;

import br.com.setis.desafiojava.domain.entity.*;
import br.com.setis.desafiojava.dto.pagamento.*;
import br.com.setis.desafiojava.exception.FalhaComunicacaoException;
import br.com.setis.desafiojava.exception.TransacaoRecusadaException;
import br.com.setis.desafiojava.mapper.ReembolsoMapper;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import br.com.setis.desafiojava.repository.LojistaRepository;
import br.com.setis.desafiojava.repository.ReembolsoRepository;
import br.com.setis.desafiojava.repository.TransacaoRepository;
import br.com.setis.desafiojava.repository.spec.TransacaoSpec;
import br.com.setis.desafiojava.service.TransacaoService;
import br.com.setis.desafiojava.strategy.ProcessadorPagamentoFactory;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransacaoServiceImpl implements TransacaoService {

  private final TransacaoRepository transacaoRepository;
  private final TransacaoMapper transacaoMapper;
  private final LojistaRepository lojistaRepository;
  private final ReembolsoRepository reembolsoRepository;
  private final ReembolsoMapper reembolsoMapper;

  private final ProcessadorPagamentoFactory processadorFactory;

  @Override
  @Transactional
  public TransacaoResponse criarTransacao(
      CriarTransacaoRequest request, String lojistaId, String solicitante) {
    var lojista =
        lojistaRepository
            .findById(UUID.fromString(lojistaId))
            .orElseThrow(
                () -> {
                  log.error("Lojista que busca criar pagamento não cadastrado: [{}]", lojistaId);
                  return new IllegalArgumentException(
                      "Lojista que busca criar pagamento não cadastrado");
                });

    if (!lojista.isAtivo()) {
      log.error("Lojista que busca criar pagamento está inativo:  [{}]", lojistaId);
      throw new IllegalArgumentException("Lojista que busca criar pagamento está inativo");
    }

    var possuiCadastroDoProvedor =
        lojista.getProvedores().contains(request.dadosPagamento().provedor());
    if (!possuiCadastroDoProvedor) {
      log.error(
          "O lojista [{}] nãote o provedor [{}] configurado",
          lojista.getCnpj(),
          request.dadosPagamento().provedor().toString());
      throw new IllegalArgumentException("O lojista não tem o provedor selecionado configurado");
    }

    var dadosPagamento = armazenarDadosDePagamento(request.dadosPagamento());

    Transacao novaTransacao =
        Transacao.builder()
            .lojista(lojista)
            .solicitante(solicitante)
            .dadosPagamento(dadosPagamento)
            .valorQuantia(transformarValor(request.valor()))
            .valorMoeda(request.moeda())
            .status(StatusTransacao.TRANSACAO_INICIADA)
            .metodoPagamento(request.metodo())
            .build();

    var transacaoSalva = transacaoRepository.saveAndFlush(novaTransacao);

    try {
      processarTransacao(transacaoSalva, request);
    } catch (TransacaoRecusadaException e) {
      log.warn("Transação negada : {}", e.getMessage());
      transacaoRepository.save(transacaoSalva);
      throw e;

    } catch (FalhaComunicacaoException | ResourceAccessException | HttpServerErrorException e) {
      log.error("Falha de comunicação com o provedor: {}", e.getMessage());
      transacaoRepository.save(transacaoSalva);
      throw e;
    } catch (Exception e) {
      log.error("Erro interno inesperado", e);
      transacaoSalva.setStatus(StatusTransacao.FALHA_COM_FORNECEDOR);
      transacaoRepository.save(transacaoSalva);
      throw e;
    }

    return transacaoMapper.toDto(transacaoRepository.save(transacaoSalva));
  }

  @Override
  public Page<TransacaoResponse> listarTransacoes(
      Pageable pageable,
      String lojistaId,
      LocalDate dataInicio,
      LocalDate dataFim,
      StatusTransacao status,
      MetodoPagamento metodo) {
    var spec = TransacaoSpec.filtrarPor(lojistaId, dataInicio, dataFim, status, metodo);
    return transacaoRepository.findAll(spec, pageable).map(transacaoMapper::toDto);
  }

  @Override
  public TransacaoResponse listarTransacaoPorId(String txId, String lojistaId) {
    return transacaoMapper.toDto(
        transacaoRepository
            .findByIdAndLojista_Id(UUID.fromString(txId), UUID.fromString(lojistaId))
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada")));
  }

  @Transactional
  @Override
  public ReembolsoResponse realizarEstorno(
      String txId, String lojistaId, String valorSolicitado, String solicitante) {
    Transacao transacao =
        transacaoRepository
            .findByIdWithLock(UUID.fromString(txId), UUID.fromString(lojistaId))
            .orElseThrow(
                () -> {
                  log.error("Transação [{}] não encontrada", txId);
                  return new IllegalArgumentException("Transação não encontrada");
                });

    if (!PERMITEM_REEMBOLSO.contains(transacao.getStatus())) {
      log.error(
          "Transação [{}] não tem status CONFIRMADA ou PARCIALMENTE_REEMBOLSADA para ser estornada",
          transacao.getId());
      throw new IllegalArgumentException(
          "Apenas transações CONFIRMADAS ou PARCIALMENTE_REEMBOLSADA podem ser estornadas.");
    }

    BigDecimal totalJaReembolsado =
        transacao.getReembolsos().stream()
            .filter(r -> r.getStatus() == StatusReembolso.CONCLUIDO)
            .map(Reembolso::getValorQuantia)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal valorOriginal = transacao.getValorQuantia();
    BigDecimal saldoDisponivel = valorOriginal.subtract(totalJaReembolsado);

    BigDecimal valorEstorno =
        (valorSolicitado != null) ? transformarValor(valorSolicitado) : saldoDisponivel;

    if (valorEstorno.compareTo(BigDecimal.ZERO) <= 0) {
      log.error("O valor do estorno deve ser maior que zero, valor solicitado: [{}]", valorEstorno);
      throw new IllegalArgumentException("O valor do estorno deve ser maior que zero.");
    }

    if (valorEstorno.compareTo(saldoDisponivel) > 0) {
      throw new IllegalArgumentException(
          String.format(
              "Saldo insuficiente para estorno. Disponível: %s, Solicitado: %s",
              saldoDisponivel, valorEstorno));
    }

    Reembolso reembolso =
        Reembolso.builder()
            .valorMoeda(transacao.getValorMoeda())
            .valorQuantia(valorEstorno)
            .status(StatusReembolso.CONCLUIDO)
            .transacao(transacao)
            .motivo("MD06")
            .build();

    if (totalJaReembolsado.add(valorEstorno).compareTo(valorOriginal) == 0) {
      transacao.setStatus(StatusTransacao.REEMBOLSADA);
    } else {
      transacao.setStatus(StatusTransacao.PARCIALMENTE_REEMBOLSADA);
    }

    var novoReembolso = reembolsoRepository.save(reembolso);

    transacao.getReembolsos().add(novoReembolso);
    transacaoRepository.save(transacao);

    return reembolsoMapper.toDto(novoReembolso);
  }

  @Override
  public Page<ReembolsoResponse> listarReembolsoPorTransacao(
      Pageable pageable, String txId, String lojistaId) {
    return reembolsoRepository
        .findAllByTxAndLojista_Id(pageable, UUID.fromString(txId), UUID.fromString(lojistaId))
        .map(reembolsoMapper::toDto);
  }

  private DadosPagamento armazenarDadosDePagamento(DadosPagamentoRequest dados) {
    return switch (dados) {
      case DadosCartaoRequest dadosCartao -> {
        String ultimos4 = dadosCartao.numero().substring(dadosCartao.numero().length() - 4);
        yield DadosPagamento.builder()
            .numeroCartaoMascarado("**** **** **** " + ultimos4)
            .nomeTitular(dadosCartao.titular())
            .provedor(dadosCartao.provedor())
            .build();
      }

      case DadosPixRequest dadosPix -> DadosPagamento.builder()
          .provedor(dadosPix.provedor())
          .chavePix(dadosPix.chavePix())
          .dataExpiracao(dadosPix.dataExpiracao())
          .build();

      case DadosBoletoRequest dadosBoleto -> DadosPagamento.builder()
          .provedor(dadosBoleto.provedor())
          .dataExpiracao(dadosBoleto.dataVencimento())
          .build();
    };
  }

  private BigDecimal transformarValor(String valor) {
    return new BigDecimal(valor).movePointLeft(2);
  }

  private void processarTransacao(Transacao transacao, CriarTransacaoRequest request) {
    var strategy = processadorFactory.get(transacao.getMetodoPagamento());
    strategy.processar(transacao, request.dadosPagamento());
  }
}
