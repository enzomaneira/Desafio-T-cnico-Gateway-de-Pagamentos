package br.com.setis.desafiojava.utils;

import br.com.setis.desafiojava.domain.entity.Cargo;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationParseJwt {

  public List<String> obterRoles(Jwt jwt) {
    if (jwt == null) return List.of();

    try {
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess == null) return List.of();

      @SuppressWarnings("unchecked")
      List<String> roles = (List<String>) realmAccess.get("roles");

      return roles != null ? roles : List.of();
    } catch (Exception e) {
      log.error("Erro ao extrair roles do JWT: {}", e.getMessage(), e);
      return List.of();
    }
  }

  public Cargo obterCargoBaseadoEmRoles(Jwt jwt) {
    if (jwt == null) return null;

    List<String> roles = obterRoles(jwt);

    return Arrays.stream(Cargo.values())
        .filter(cargo -> roles.contains(cargo.name()))
        .findFirst()
        .orElse(null);
  }

  public String obterMerchantId(Jwt jwt) {
    if (jwt == null) return null;
    return jwt.getClaimAsString("merchant_id");
  }

  public String obterUsername(Jwt jwt) {
    if (jwt == null) return null;
    return jwt.getClaimAsString("preferred_username");
  }
}
