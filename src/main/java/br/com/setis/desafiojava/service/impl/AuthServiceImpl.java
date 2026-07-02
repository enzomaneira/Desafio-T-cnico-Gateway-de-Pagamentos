package br.com.setis.desafiojava.service.impl;

import br.com.setis.desafiojava.domain.entity.UsuarioLojista;
import br.com.setis.desafiojava.dto.auth.TokenResponse;
import br.com.setis.desafiojava.repository.UsuarioLojistaRepository;
import br.com.setis.desafiojava.service.AuthService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UsuarioLojistaRepository usuarioLojistaRepository;
  private final RestTemplate restTemplate;

  @Value("${keycloak.server-url}")
  private String keycloakServerUrl;

  @Value("${keycloak.realm}")
  private String realm;

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.client-secret}")
  private String clientSecret;

  @Override
  public TokenResponse gerarToken(String email, String password) {
    var usuario =
        usuarioLojistaRepository
            .findByEmail(email)
            .orElseThrow(
                () -> {
                  log.warn("Usuário não cadastrado no sistema: {}", email);
                  return new IllegalArgumentException("Usuário não cadastrado no sistema.");
                });

    if (!usuario.isAtivo()) {
      log.warn("Usuário está inativo: {}", email);
      throw new IllegalArgumentException("Usuário está inativo");
    }

    String url =
        String.format("%s/realms/%s/protocol/openid-connect/token", keycloakServerUrl, realm);

    MultiValueMap<String, String> headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("grant_type", "password");
    formData.add("username", email);
    formData.add("password", password);
    formData.add("scope", "openid");

    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

    try {
      ResponseEntity<TokenResponse> response =
          restTemplate.postForEntity(url, requestEntity, TokenResponse.class);
      atualizarUltimoLogin(usuario);
      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.error(
          "Erro ao autenticar no Keycloak: Status {}, Body {}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new IllegalStateException("Falha na autenticação: " + e.getResponseBodyAsString(), e);
    }
  }

  @Transactional
  @Async
  protected void atualizarUltimoLogin(
      UsuarioLojista usuario) { // Protected para facilitar testes se necessário, ou manter private
    usuario.setUltimoLogin(LocalDateTime.now());
    usuarioLojistaRepository.save(usuario);
    if (log.isInfoEnabled()) {
      log.info("novo login realizado para o usuário: {}", usuario.getEmail());
    }
  }
}
