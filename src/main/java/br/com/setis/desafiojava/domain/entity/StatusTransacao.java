package br.com.setis.desafiojava.domain.entity;

import java.util.Set;

public enum StatusTransacao {
  TRANSACAO_INICIADA,
  AGUARDANDO_PAGAMENTO,
  CONFIRMADA,
  FALHA_COM_FORNECEDOR,
  EXPIRADO,
  CANCELADA,
  NEGADA,
  REEMBOLSADA,
  PARCIALMENTE_REEMBOLSADA;

  public static final Set<StatusTransacao> PERMITEM_REEMBOLSO =
      Set.of(CONFIRMADA, PARCIALMENTE_REEMBOLSADA);
}
