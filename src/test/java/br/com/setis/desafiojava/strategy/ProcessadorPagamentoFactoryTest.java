package br.com.setis.desafiojava.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.strategy.impl.ProcessarBoleto;
import br.com.setis.desafiojava.strategy.impl.ProcessarCartaoCredito;
import br.com.setis.desafiojava.strategy.impl.ProcessarPix;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessadorPagamentoFactoryTest {

  @Mock private ProcessarPix processarPix;
  @Mock private ProcessarBoleto processarBoleto;
  @Mock private ProcessarCartaoCredito processarCartaoCredito;

  @Test
  @DisplayName("Deve retornar a estratégia correta para cada método de pagamento")
  void deveRetornarEstrategiaCorreta() {
    when(processarPix.getMetodo()).thenReturn(MetodoPagamento.PIX);
    when(processarBoleto.getMetodo()).thenReturn(MetodoPagamento.BOLETO);
    when(processarCartaoCredito.getMetodo()).thenReturn(MetodoPagamento.CARTAO_CREDITO);

    ProcessadorPagamentoFactory factory =
        new ProcessadorPagamentoFactory(
            Set.of(processarPix, processarBoleto, processarCartaoCredito));

    assertEquals(processarPix, factory.get(MetodoPagamento.PIX));
    assertEquals(processarBoleto, factory.get(MetodoPagamento.BOLETO));
    assertEquals(processarCartaoCredito, factory.get(MetodoPagamento.CARTAO_CREDITO));
  }

  @Test
  @DisplayName("Deve lançar erro se não houver estratégia implementada")
  void deveLancarErroEstrategiaInexistente() {
    ProcessadorPagamentoFactory factory = new ProcessadorPagamentoFactory(Set.of());

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> factory.get(MetodoPagamento.PIX));

    assertEquals("Nenhuma estratégia implementada para o método: PIX", ex.getMessage());
  }
}
