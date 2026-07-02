package br.com.setis.desafiojava.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.setis.desafiojava.domain.entity.UsuarioLojista;
import br.com.setis.desafiojava.dto.auth.TokenResponse;
import br.com.setis.desafiojava.repository.UsuarioLojistaRepository;
import br.com.setis.desafiojava.service.impl.AuthServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
  @Mock private UsuarioLojistaRepository usuarioLojistaRepository;
  @Mock private RestTemplate restTemplate;

  @InjectMocks private AuthServiceImpl authService;

  @Test
  @DisplayName("Deve gerar um token de acesso corretamente para o usuário cadastrado e ativo")
  void deveGerarToken() {
    String email = "teste@teste.com";
    ReflectionTestUtils.setField(authService, "keycloakServerUrl", "http://keycloak");
    ReflectionTestUtils.setField(authService, "realm", "teste-realm");
    ReflectionTestUtils.setField(authService, "clientId", "meu-client");
    ReflectionTestUtils.setField(authService, "clientSecret", "123");

    UsuarioLojista usuarioLojista = UsuarioLojista.builder().email(email).ativo(true).build();

    TokenResponse token =
        new TokenResponse("token123", 100, 100, "refreshToken", "tokenType", "scope");

    ResponseEntity<TokenResponse> responseEntity = ResponseEntity.ok(token);

    when(usuarioLojistaRepository.findByEmail(email)).thenReturn(Optional.of(usuarioLojista));
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
        .thenReturn(responseEntity);

    TokenResponse response = authService.gerarToken(email, "senha");

    Assertions.assertNotNull(response);
    Assertions.assertEquals("token123", response.accessToken());
    Assertions.assertEquals(100, response.expiresIn());
    Assertions.assertEquals(100, response.refreshExpiresIn());
    Assertions.assertEquals("refreshToken", response.refreshToken());
    Assertions.assertEquals("tokenType", response.tokenType());
    Assertions.assertEquals("scope", response.scope());

    verify(usuarioLojistaRepository).findByEmail(email);
    verify(usuarioLojistaRepository).save(usuarioLojista);
  }

  @Test
  @DisplayName("Deve lançar exceção caso usuário não esteja cadastrado no sistema")
  void deveFalharCasoUsuarioNaoExiste() {
    String email = "teste@teste.com";
    ReflectionTestUtils.setField(authService, "keycloakServerUrl", "http://keycloak");
    ReflectionTestUtils.setField(authService, "realm", "teste-realm");
    ReflectionTestUtils.setField(authService, "clientId", "meu-client");
    ReflectionTestUtils.setField(authService, "clientSecret", "123");

    when(usuarioLojistaRepository.findByEmail(email)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.gerarToken(email, "senha"));

    Assertions.assertEquals("Usuário não cadastrado no sistema.", exception.getMessage());
    verify(usuarioLojistaRepository).findByEmail(email);
  }

  @Test
  @DisplayName("Deve lançar exceção caso o usuário esteja inativo")
  void deveFalharCasoUsuarioEstejaInativo() {
    String email = "teste@teste.com";
    ReflectionTestUtils.setField(authService, "keycloakServerUrl", "http://keycloak");
    ReflectionTestUtils.setField(authService, "realm", "teste-realm");
    ReflectionTestUtils.setField(authService, "clientId", "meu-client");
    ReflectionTestUtils.setField(authService, "clientSecret", "123");

    UsuarioLojista usuarioLojista = UsuarioLojista.builder().email(email).ativo(false).build();

    when(usuarioLojistaRepository.findByEmail(email)).thenReturn(Optional.of(usuarioLojista));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> authService.gerarToken(email, "senha"));

    Assertions.assertEquals("Usuário está inativo", exception.getMessage());
    verify(usuarioLojistaRepository).findByEmail(email);
  }

  @Test
  @DisplayName(
      "Deve lançar RuntimeException quando o Keycloak retornar erro (ex: 401 Unauthorized)")
  void deveTratarErroDoKeycloak() {
    String email = "teste@teste.com";
    ReflectionTestUtils.setField(authService, "keycloakServerUrl", "http://keycloak");

    UsuarioLojista usuarioLojista = UsuarioLojista.builder().email(email).ativo(true).build();

    when(usuarioLojistaRepository.findByEmail(email)).thenReturn(Optional.of(usuarioLojista));

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
        .thenThrow(
            new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> authService.gerarToken(email, "senha-errada"));

    Assertions.assertTrue(exception.getMessage().contains("Falha na autenticação"));
    verify(usuarioLojistaRepository, org.mockito.Mockito.never()).save(any());
  }
}
