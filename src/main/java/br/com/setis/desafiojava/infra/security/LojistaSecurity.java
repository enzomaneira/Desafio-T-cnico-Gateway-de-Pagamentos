package br.com.setis.desafiojava.infra.security;

import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component("lojistaSecurity")
@RequiredArgsConstructor
public class LojistaSecurity {

  private final AuthenticationParseJwt authenticationParseJwt;

  public boolean podeGerenciarLojista(String lojistaId, Jwt jwt) {
    if (jwt == null) return false;

    if (ehAdmin(jwt)) {
      return true;
    }

    if (ehGerente(jwt)) {
      String merchantIdToken = authenticationParseJwt.obterMerchantId(jwt);
      return Objects.equals(lojistaId, merchantIdToken);
    }

    return false;
  }

  private boolean ehAdmin(Jwt jwt) {
    return temRole(jwt, "ADMINISTRADOR");
  }

  private boolean ehGerente(Jwt jwt) {
    return temRole(jwt, "GERENTE");
  }

  private boolean temRole(Jwt jwt, String roleDesejada) {
    try {
      List<String> roles = authenticationParseJwt.obterRoles(jwt);
      return roles != null && roles.contains(roleDesejada);
    } catch (IllegalArgumentException e) {
      log.warn("Falha ao validar roles do JWT: {}", e.getMessage());
      return false;
    }
  }
}
