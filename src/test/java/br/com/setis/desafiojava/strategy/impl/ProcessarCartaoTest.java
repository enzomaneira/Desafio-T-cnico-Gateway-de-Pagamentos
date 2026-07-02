package br.com.setis.desafiojava.strategy.impl;

import static org.junit.jupiter.api.Assertions.*;

import br.com.setis.desafiojava.domain.entity.Provedor;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.dto.pagamento.DadosCartaoRequest;
import br.com.setis.desafiojava.exception.TransacaoRecusadaException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessarCartaoTest {

  @InjectMocks private ProcessarCartaoCredito processarCartao;

  private Transacao transacao;

  @BeforeEach
  void setup() {
    transacao = new Transacao();
    transacao.setId(UUID.randomUUID());
    transacao.setValor(Money.of(new BigDecimal("100.00"), "BRL"));
    transacao.setDadosPagamento(new br.com.setis.desafiojava.domain.entity.DadosPagamento());
  }

  @Test
  @DisplayName("Deve passar com cartão válido")
  void deveProcessarCartaoValido() {
    String cartaoValido = "49927398716";
    String validadeFutura =
        YearMonth.now().plusYears(1).format(DateTimeFormatter.ofPattern("MM/yy"));

    DadosCartaoRequest request =
        new DadosCartaoRequest(cartaoValido, "teste", validadeFutura, "131", Provedor.CIELO);

    assertDoesNotThrow(() -> processarCartao.processar(transacao, request));
  }

  @Test
  @DisplayName("Deve recusar cartão com Luhn inválido")
  void deveRecusarLuhnInvalido() {
    String cartaoInvalido = "1234567890123456";
    DadosCartaoRequest request =
        new DadosCartaoRequest(cartaoInvalido, "teste", "10/30", "131", Provedor.CIELO);
    ;

    TransacaoRecusadaException ex =
        assertThrows(
            TransacaoRecusadaException.class, () -> processarCartao.processar(transacao, request));
    assertTrue(ex.getMessage().contains("Checksum"));
  }

  @Test
  @DisplayName("Deve recusar cartão vencido")
  void deveRecusarCartaoVencido() {
    String cartaoValido = "49927398716";
    String validadePassada =
        YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MM/yy"));

    DadosCartaoRequest request =
        new DadosCartaoRequest(cartaoValido, "teste", validadePassada, "131", Provedor.CIELO);
    ;

    TransacaoRecusadaException ex =
        assertThrows(
            TransacaoRecusadaException.class, () -> processarCartao.processar(transacao, request));
    assertTrue(ex.getMessage().contains("vencido"));
  }

  @Test
  @DisplayName("Deve recusar CVV 999 (Simulação de erro)")
  void deveRecusarCvvSimulado() {
    String cartaoValido = "49927398716";
    String validadeFutura =
        YearMonth.now().plusYears(1).format(DateTimeFormatter.ofPattern("MM/yy"));

    DadosCartaoRequest request =
        new DadosCartaoRequest(cartaoValido, "teste", validadeFutura, "999", Provedor.CIELO);
    ;

    TransacaoRecusadaException ex =
        assertThrows(
            TransacaoRecusadaException.class, () -> processarCartao.processar(transacao, request));
    assertTrue(ex.getMessage().contains("CVV Inválido"));
  }
}
