package br.com.setis.desafiojava.exception;

import br.com.setis.desafiojava.domain.entity.Transacao;
import java.io.Serial;
import lombok.Getter;

@Getter
public class FalhaComunicacaoException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final Transacao transacao;

  public FalhaComunicacaoException(String message, Throwable cause, Transacao transacao) {
    super(message, cause);
    this.transacao = transacao;
  }
}
