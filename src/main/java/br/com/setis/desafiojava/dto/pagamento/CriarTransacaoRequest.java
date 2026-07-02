package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.dto.validation.ProvedorValido;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ProvedorValido
public record CriarTransacaoRequest(
    @NotBlank
        @Pattern(
            regexp = "^\\d+$",
            message = "O valor deve ser em centavos (apenas números, sem pontos ou vírgulas)")
        String valor,
    @NotBlank String moeda,
    @NotNull MetodoPagamento metodo,
    @Valid
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "metodo")
        @JsonSubTypes({
          @JsonSubTypes.Type(
              value = DadosCartaoRequest.class,
              names = {"CARTAO_CREDITO", "CARTAO_DEBITO"}),
          @JsonSubTypes.Type(
              value = DadosPixRequest.class,
              names = {"PIX"}),
          @JsonSubTypes.Type(
              value = DadosBoletoRequest.class,
              names = {"BOLETO"})
        })
        DadosPagamentoRequest dadosPagamento) {}
