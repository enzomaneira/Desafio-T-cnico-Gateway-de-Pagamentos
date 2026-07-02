package br.com.setis.desafiojava.dto.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"erro", "violacoes"})
public class BadRequest {
  private final String erro;
  private final List<Violacao> violacoes;

  public BadRequest(List<Violacao> violacoes) {
    this.erro = "Bad Request";
    this.violacoes = violacoes;
  }

  public record Violacao(String campo, String violacao) {}
}
