package br.com.setis.desafiojava.strategy.impl;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.dto.pagamento.DadosPagamentoRequest;
import br.com.setis.desafiojava.dto.pagamento.DadosPixRequest;
import br.com.setis.desafiojava.repository.TransacaoRepository;
import br.com.setis.desafiojava.strategy.ProcessadorDePagamentos;
import br.com.setis.desafiojava.strategy.ProcessadorPagamentoStrategy;
import br.com.setis.desafiojava.utils.PixUtils;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessarPix extends ProcessadorDePagamentos implements ProcessadorPagamentoStrategy {

  private final TransacaoRepository transacaoRepository;

  @Override
  public MetodoPagamento getMetodo() {
    return MetodoPagamento.PIX;
  }

  @Override
  public void processar(Transacao transacao, DadosPagamentoRequest request) {
    DadosPixRequest dadosPix = (DadosPixRequest) request;
    var lojista = transacao.getLojista();

    BigDecimal valor = transacao.getValorQuantia();
    String chavePix = dadosPix.chavePix();
    String nomeFantasia = lojista.getNomeFantasia();

    // formato bacen precisa remover os hífens e no máximo 25 caracteres
    String txId = transacao.getId().toString().replace("-", "").substring(0, 25);

    String emvCode = PixUtils.gerarEmvCopiaCola(chavePix, nomeFantasia, valor, txId);

    transacao.getDadosPagamento().setPixQrCodeBase64(emvCode);
    transacao.getDadosPagamento().setChavePix(chavePix);
    transacao.setIdTransacaoPsp(txId);
    transacao.setStatus(StatusTransacao.AGUARDANDO_PAGAMENTO);
    transacao.setRespostaPspPura("201 - OK");

    log.info(
        "Pix Copia e Cola gerado para transação [{}]. Aguardando pagamento", transacao.getId());

    simularPagamentoAssincrono(transacao);
  }

  private void simularPagamentoAssincrono(Transacao transacao) {
    CompletableFuture.runAsync(
        () -> {
          try {
            TimeUnit.SECONDS.sleep(30);

            log.info("Simulando pagamento do Pix para transação: {}", transacao.getId());

            super.simularProcessamentoExterno(transacao);

            transacaoRepository.save(transacao);

            log.info(
                "Transação Pix {} finalizada com status: {}",
                transacao.getId(),
                transacao.getStatus());

          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Erro na simulação do Pix", e);
          }
        });
  }
}
