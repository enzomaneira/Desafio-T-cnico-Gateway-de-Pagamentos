package br.com.setis.desafiojava.strategy;

import static org.junit.jupiter.api.Assertions.*;

import br.com.setis.desafiojava.domain.entity.DadosPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.exception.FalhaComunicacaoException;
import br.com.setis.desafiojava.exception.TransacaoRecusadaException;
import java.math.BigDecimal;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProcessadorDePagamentosTest {

  private static class ProcessadorTeste extends ProcessadorDePagamentos {
    @Override
    public void simularProcessamentoExterno(Transacao transacao) {
      super.simularProcessamentoExterno(transacao);
    }
  }

  private ProcessadorTeste processador;
  private Transacao transacao;

  @BeforeEach
  void setup() {
    processador = new ProcessadorTeste();
    transacao = new Transacao();
    transacao.setDadosPagamento(new DadosPagamento());
  }

  @Test
  @DisplayName("Final 01: Deve recusar por Saldo Insuficiente")
  void deveRecusarSaldoInsuficiente() {
    transacao.setValor(Money.of(new BigDecimal("100.01"), "BRL"));

    TransacaoRecusadaException ex =
        assertThrows(
            TransacaoRecusadaException.class,
            () -> processador.simularProcessamentoExterno(transacao));

    assertEquals("Saldo Insuficiente", ex.getMessage());
    assertEquals(StatusTransacao.NEGADA, transacao.getStatus());
    assertEquals("Saldo Insuficiente", transacao.getRespostaPspPura());
  }

  @Test
  @DisplayName("Final 02: Deve recusar por Bloqueio Antifraude")
  void deveRecusarAntifraude() {
    transacao.setValor(Money.of(new BigDecimal("50.02"), "BRL"));

    TransacaoRecusadaException ex =
        assertThrows(
            TransacaoRecusadaException.class,
            () -> processador.simularProcessamentoExterno(transacao));

    assertEquals("Bloqueio Antifraude", ex.getMessage());
    assertEquals(StatusTransacao.NEGADA, transacao.getStatus());
  }

  @Test
  @DisplayName("Final 03: Deve lançar Falha de Comunicação")
  void deveLancarFalhaComunicacao() {
    transacao.setValor(Money.of(new BigDecimal("10.03"), "BRL"));

    FalhaComunicacaoException ex =
        assertThrows(
            FalhaComunicacaoException.class,
            () -> processador.simularProcessamentoExterno(transacao));

    assertEquals("Fornecedor indisponível", ex.getMessage());
    assertEquals(StatusTransacao.FALHA_COM_FORNECEDOR, transacao.getStatus());
  }

  @Test
  @DisplayName("Outros Finais: Deve Aprovar com Sucesso")
  void deveAprovarTransacao() {
    transacao.setValor(Money.of(new BigDecimal("99.90"), "BRL"));

    processador.simularProcessamentoExterno(transacao);

    assertEquals(StatusTransacao.CONFIRMADA, transacao.getStatus());
    assertEquals("Transação Aprovada com Sucesso", transacao.getRespostaPspPura());
    assertNotNull(transacao.getDadosPagamento().getDataPagamento());
    assertNotNull(transacao.getDadosPagamento().getE2eId());
  }
}
