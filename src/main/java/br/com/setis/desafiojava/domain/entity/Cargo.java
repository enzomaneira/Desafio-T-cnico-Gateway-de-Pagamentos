package br.com.setis.desafiojava.domain.entity;

import java.util.Set;

public enum Cargo {
  ADMINISTRADOR,
  SUPORTE,
  GERENTE,
  ANALISTA;

  public static final Set<Cargo> CARGOS_PERMITIDOS_GERENTE = Set.of(GERENTE, ANALISTA);
  public static final Set<Cargo> CARGOS_PERMITIDOS_ON_BEHALF = Set.of(ADMINISTRADOR, SUPORTE);
}
