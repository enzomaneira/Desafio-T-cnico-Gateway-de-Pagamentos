package br.com.setis.desafiojava.infra.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class LojistaSecurityTest {

  @Mock private AuthenticationParseJwt authenticationParseJwt;

  @InjectMocks private LojistaSecurity lojistaSecurity;

  @Test
  @DisplayName("Deve permitir acesso total para ADMINISTRADOR")
  void devePermitirAdminSempre() {
    String lojistaId = "123";
    Jwt jwt = mock(Jwt.class);

    when(authenticationParseJwt.obterRoles(jwt)).thenReturn(List.of("ADMINISTRADOR"));

    boolean resultado = lojistaSecurity.podeGerenciarLojista(lojistaId, jwt);

    assertTrue(resultado, "Admin deve ter acesso a qualquer lojista");
  }

  @Test
  @DisplayName("Deve permitir Gerente apenas se o ID do lojista bater com o token")
  void devePermitirGerenteComIdCorreto() {
    String idLojista = "lojista-abc";
    Jwt jwt = mock(Jwt.class);

    when(authenticationParseJwt.obterRoles(jwt)).thenReturn(List.of("GERENTE"));
    when(authenticationParseJwt.obterMerchantId(jwt)).thenReturn("lojista-abc");

    boolean resultado = lojistaSecurity.podeGerenciarLojista(idLojista, jwt);

    assertTrue(resultado, "Gerente deve acessar seu próprio lojista");
  }

  @Test
  @DisplayName("Deve bloquear Gerente se tentar acessar outro lojista")
  void deveBloquearGerenteComIdIncorreto() {
    String idAlvo = "lojista-alvo";
    String idToken = "lojista-origem";
    Jwt jwt = mock(Jwt.class);

    when(authenticationParseJwt.obterRoles(jwt)).thenReturn(List.of("GERENTE"));
    when(authenticationParseJwt.obterMerchantId(jwt)).thenReturn(idToken);

    boolean resultado = lojistaSecurity.podeGerenciarLojista(idAlvo, jwt);

    assertFalse(resultado, "Gerente não pode acessar loja de outro merchant");
  }

  @Test
  @DisplayName("Deve bloquear acesso se token não tiver role permitida (Ex: Vendedor)")
  void deveBloquearRoleSemPermissao() {
    Jwt jwt = mock(Jwt.class);

    when(authenticationParseJwt.obterRoles(jwt)).thenReturn(List.of("VENDEDOR"));

    boolean resultado = lojistaSecurity.podeGerenciarLojista("qualquer-id", jwt);

    assertFalse(resultado);
  }

  @Test
  @DisplayName("Deve bloquear se JWT for null")
  void deveBloquearJwtNull() {
    assertFalse(lojistaSecurity.podeGerenciarLojista("123", null));
  }
}
