package br.com.setis.desafiojava.strategy.impl;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import org.springframework.stereotype.Component;

@Component
public class ProcessarCartaoDebito extends ProcessarCartao {
  @Override
  public MetodoPagamento getMetodo() {
    return MetodoPagamento.CARTAO_DEBITO;
  }
}
