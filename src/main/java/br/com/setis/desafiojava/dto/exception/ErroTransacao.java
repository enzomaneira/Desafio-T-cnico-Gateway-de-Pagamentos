package br.com.setis.desafiojava.dto.exception;

import br.com.setis.desafiojava.dto.pagamento.TransacaoResponse;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"erro", "motivo", "transacao"})
public record ErroTransacao(String erro, String motivo, TransacaoResponse transacao) {}
