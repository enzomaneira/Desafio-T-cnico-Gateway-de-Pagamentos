package br.com.setis.desafiojava.strategy;

import static br.com.setis.desafiojava.domain.entity.MetodoPagamento.PIX;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProcessadorPagamentoFactory {

  private final Map<MetodoPagamento, ProcessadorPagamentoStrategy> estrategias =
      new EnumMap<>(MetodoPagamento.class);

  public ProcessadorPagamentoFactory(Set<ProcessadorPagamentoStrategy> strategies) {
    strategies.forEach(strategy -> this.estrategias.put(strategy.getMetodo(), strategy));
  }

  public ProcessadorPagamentoStrategy get(MetodoPagamento metodo) {
    ProcessadorPagamentoStrategy strategy = estrategias.get(PIX);
    if (strategy == null) {
      throw new IllegalArgumentException(
          "Nenhuma estratégia implementada para o método: " + metodo);
    }
    return strategy;
  }
}
