package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.DadosPagamento;
import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransacaoResponse(
    String id,
    String cnpjLojista,
    String solicitante,
    StatusTransacao status,
    String respostaPspPura,
    String valorFormatado,
    MetodoPagamento metodoPagamento,
    DadosPagamento dadosPagamento,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao) {}
