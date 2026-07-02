package br.com.setis.desafiojava.exception;

import br.com.setis.desafiojava.domain.entity.Transacao;
import java.io.Serial;
import lombok.Getter;

@Getter
public class TransacaoRecusadaException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final Transacao transacao;

  public TransacaoRecusadaException(String mensagem, Transacao transacao) {
    super(mensagem);
    this.transacao = transacao;
  }
}
