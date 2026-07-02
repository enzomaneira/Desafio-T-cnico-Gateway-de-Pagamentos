package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.Provedor;

public sealed interface DadosPagamentoRequest
    permits DadosCartaoRequest, DadosPixRequest, DadosBoletoRequest {
  Provedor provedor();
}
