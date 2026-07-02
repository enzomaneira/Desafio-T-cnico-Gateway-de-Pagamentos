package br.com.setis.desafiojava.domain.entity;

import java.util.Set;

public enum Provedor {
  C6BANK,
  CIELO,
  ITAU,
  BRADESCO,
  SICOOB,
  GETNET,
  REDE,
  STONE,
  SICREDI;

  public static final Set<Provedor> ACEITAM_PIX =
      Set.of(C6BANK, CIELO, ITAU, SICOOB, SICREDI, BRADESCO);
  public static final Set<Provedor> ACEITAM_BOLETO = Set.of(BRADESCO, ITAU);
  public static final Set<Provedor> ACEITAM_CARTAO = Set.of(GETNET, REDE, STONE, CIELO);

  public boolean suporta(MetodoPagamento metodo) {
    if (metodo == null) return false;

    return switch (metodo) {
      case PIX -> ACEITAM_PIX.contains(this);
      case BOLETO -> ACEITAM_BOLETO.contains(this);
      case CARTAO_CREDITO, CARTAO_DEBITO -> ACEITAM_CARTAO.contains(this);
    };
  }
}
