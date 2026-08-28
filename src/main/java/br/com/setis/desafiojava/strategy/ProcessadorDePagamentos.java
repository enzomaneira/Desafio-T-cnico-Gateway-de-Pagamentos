package br.com.setis.desafiojava.strategy;

import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.exception.FalhaComunicacaoException;
import br.com.setis.desafiojava.exception.TransacaoRecusadaException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

public abstract class ProcessadorDePagamentos {

  protected void simularProcessamentoExterno(Transacao transacao) {
    int centavos =
        transacao
            .getValorQuantia()
            .remainder(BigDecimal.ONE)
            .movePointRight(2)
            .setScale(0, RoundingMode.DOWN)
            .intValue();

    switch (centavos) {
      case 1 -> {
        transacao.setStatus(StatusTransacao.NEGADA);
        transacao.setRespostaPspPura("Saldo Insuficiente");
        throw new TransacaoRecusadaException("Saldo Insuficiente", transacao);
      }
      case 2 -> {
        transacao.setStatus(StatusTransacao.NEGADA);
        transacao.setRespostaPspPura("Bloqueio Antifraude");
        throw new TransacaoRecusadaException("Bloqueio Antifraude", transacao);
      }
      case 3 -> {
        transacao.setStatus(StatusTransacao.FALHA_COM_FORNECEDOR);
        Exception causa =
            new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no fornecedor");
        throw new FalhaComunicacaoException("Fornecedor indisponível", causa, transacao);
      }
      default -> {
        transacao.setStatus(StatusTransacao.CONFIRMADA);
        transacao.getDadosPagamento().setDataPagamento(LocalDateTime.now());
        transacao.getDadosPagamento().setE2eId(UUID.randomUUID().toString());
        transacao.setRespostaPspPura("Transação Aprovada com Sucesso");
      }
    }
  }
}
