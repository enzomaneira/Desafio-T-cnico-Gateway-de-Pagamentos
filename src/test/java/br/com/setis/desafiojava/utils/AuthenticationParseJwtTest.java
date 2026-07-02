package br.com.setis.desafiojava.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AuthenticationParseJwtTest {

  @Mock private Jwt jwt;

  @InjectMocks private AuthenticationParseJwt parseJwt;

  @Test
  @DisplayName("Deve extrair lista de roles corretamente quando existem")
  void deveExtrairRolesCorretamente() {
    Map<String, Object> realmAccessMap = Map.of("roles", List.of("ADMIN", "GERENTE"));

    when(jwt.getClaimAsMap("realm_access")).thenReturn(realmAccessMap);

    List<String> roles = parseJwt.obterRoles(jwt);

    assertNotNull(roles);
    assertEquals(2, roles.size());
    assertTrue(roles.contains("ADMIN"));
    assertTrue(roles.contains("GERENTE"));
  }

  @Test
  @DisplayName("Deve retornar lista vazia se JWT for null")
  void deveRetornarVazioSeJwtNull() {
    List<String> roles = parseJwt.obterRoles(null);
    assertTrue(roles.isEmpty());
  }

  @Test
  @DisplayName("Deve retornar vazio se claim realm_access não existir")
  void deveRetornarVazioSeRealmAccessNull() {
    when(jwt.getClaimAsMap("realm_access")).thenReturn(null);

    List<String> roles = parseJwt.obterRoles(jwt);
    assertTrue(roles.isEmpty());
  }

  @Test
  @DisplayName("Deve retornar vazio se campo roles não existir dentro de realm_access")
  void deveRetornarVazioSeRolesNull() {
    Map<String, Object> realmAccessVazio = Map.of("outra_coisa", "valor");
    when(jwt.getClaimAsMap("realm_access")).thenReturn(realmAccessVazio);

    List<String> roles = parseJwt.obterRoles(jwt);
    assertTrue(roles.isEmpty());
  }

  @Test
  @DisplayName("Deve tratar exceção e retornar lista vazia se houver erro no cast")
  void deveTratarErroDeCast() {
    Map<String, Object> mapaInvalido = Map.of("roles", "não-sou-uma-lista");
    when(jwt.getClaimAsMap("realm_access")).thenReturn(mapaInvalido);

    List<String> roles = parseJwt.obterRoles(jwt);

    assertTrue(roles.isEmpty());
  }

  @Test
  @DisplayName("Deve extrair merchant_id corretamente")
  void deveExtrairMerchantId() {
    String idEsperado = "lojista-123";
    when(jwt.getClaimAsString("merchant_id")).thenReturn(idEsperado);

    String resultado = parseJwt.obterMerchantId(jwt);

    assertEquals(idEsperado, resultado);
  }

  @Test
  @DisplayName("Deve retornar null se merchant_id não existir")
  void deveRetornarNullSeMerchantIdAusente() {
    when(jwt.getClaimAsString("merchant_id")).thenReturn(null);

    String resultado = parseJwt.obterMerchantId(jwt);
    assertNull(resultado);
  }
}
