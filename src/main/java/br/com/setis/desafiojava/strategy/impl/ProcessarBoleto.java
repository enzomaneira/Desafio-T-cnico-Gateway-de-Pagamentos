package br.com.setis.desafiojava.strategy.impl;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.dto.pagamento.DadosBoletoRequest;
import br.com.setis.desafiojava.dto.pagamento.DadosPagamentoRequest;
import br.com.setis.desafiojava.repository.TransacaoRepository;
import br.com.setis.desafiojava.strategy.ProcessadorDePagamentos;
import br.com.setis.desafiojava.strategy.ProcessadorPagamentoStrategy;
import br.com.setis.desafiojava.utils.BoletoUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessarBoleto extends ProcessadorDePagamentos
    implements ProcessadorPagamentoStrategy {
  private final TransacaoRepository transacaoRepository;

  @Override
  public MetodoPagamento getMetodo() {
    return MetodoPagamento.BOLETO;
  }

  @Override
  public void processar(Transacao transacao, DadosPagamentoRequest request) {
    DadosBoletoRequest dadosBoleto = (DadosBoletoRequest) request;

    String codigoBarras =
        BoletoUtils.gerarCodigoBarras(
            dadosBoleto.provedor(), transacao.getValorQuantia(), dadosBoleto.dataVencimento());

    transacao.getDadosPagamento().setCodigoBarrasBoleto(codigoBarras);
    transacao.getDadosPagamento().setDataExpiracao(dadosBoleto.dataVencimento());
    transacao.setStatus(StatusTransacao.AGUARDANDO_PAGAMENTO);
    transacao.setRespostaPspPura("201 - OK");

    log.info("Boleto gerado para transação [{}]. Aguardando pagamento", transacao.getId());

    simularPagamentoAssincrono(transacao);
  }

  private void simularPagamentoAssincrono(Transacao transacao) {
    CompletableFuture.runAsync(
        () -> {
          try {
            TimeUnit.SECONDS.sleep(30);
            log.info("Simulando compensação do Boleto: {}", transacao.getId());

            /*todo
              aqui está faltando a lógica básica deste método
              ele tem que salvar a transação e fazer a simulação da chamada de API
            */

            log.info(
                "Boleto {} processado. Status Final: {}", transacao.getId(), transacao.getStatus());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Erro na simulação do boleto", e);
          }
        });
  }
}
