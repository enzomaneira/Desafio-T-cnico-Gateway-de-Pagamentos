package br.com.setis.desafiojava.strategy;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.dto.pagamento.DadosPagamentoRequest;

public interface ProcessadorPagamentoStrategy {
  MetodoPagamento getMetodo();

  void processar(Transacao transacao, DadosPagamentoRequest request);
}
