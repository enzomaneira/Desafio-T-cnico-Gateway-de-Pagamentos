package br.com.setis.desafiojava.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import javax.money.MonetaryAmount;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyFormatterTest {
  @Test
  @DisplayName("Deve formatar valor positivo corretamente para BRL")
  void deveFormatarValorPositivo() {
    MonetaryAmount valor = Money.of(new BigDecimal("1000.50"), "BRL");

    String resultado = CurrencyFormatter.format(valor).replace("\u00A0", " ");

    assertTrue(resultado.contains("R$"));
    assertTrue(resultado.contains("1.000,50"));
  }

  @Test
  @DisplayName("Deve formatar valor zero corretamente")
  void deveFormatarZero() {
    MonetaryAmount valor = Money.of(BigDecimal.ZERO, "BRL");

    String resultado = CurrencyFormatter.format(valor).replace("\u00A0", " ");

    assertTrue(resultado.contains("R$"));
    assertTrue(resultado.contains("0,00"));
  }

  @Test
  @DisplayName("Deve formatar centavos corretamente")
  void deveFormatarCentavos() {
    MonetaryAmount valor = Money.of(new BigDecimal("0.05"), "BRL");
    String resultado = CurrencyFormatter.format(valor).replace("\u00A0", " ");

    assertTrue(resultado.contains("0,05"));
  }

  @Test
  @DisplayName("Deve arredondar valores com mais de 2 casas decimais")
  void deveArredondarCasasDecimais() {
    MonetaryAmount valorPraCima = Money.of(new BigDecimal("10.559"), "BRL");
    MonetaryAmount valorPraBaixo = Money.of(new BigDecimal("10.551"), "BRL");

    String arredondadoPraCima = CurrencyFormatter.format(valorPraCima);
    String arredondadoPraBaixo = CurrencyFormatter.format(valorPraBaixo);

    assertTrue(arredondadoPraCima.contains("10,56"));
    assertTrue(arredondadoPraBaixo.contains("10,55"));
  }

  @Test
  @DisplayName("Deve formatar valores negativos corretamente")
  void deveFormatarNegativo() {
    MonetaryAmount valorNegativo = Money.of(new BigDecimal("-50.00"), "BRL");

    String resultado = CurrencyFormatter.format(valorNegativo).replace("\u00A0", " ");

    assertTrue(resultado.contains("-"));
    assertTrue(resultado.contains("50,00"));
  }

  @Test
  @DisplayName("Deve lidar com valor nulo (Retornar string vazia ou lançar erro)")
  void deveLidarComNulo() {
    String resultado = CurrencyFormatter.format(null);
    assertNull(resultado, "Se a implementação retorna null para input null");
  }
}
