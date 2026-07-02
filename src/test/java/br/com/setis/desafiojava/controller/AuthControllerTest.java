package br.com.setis.desafiojava.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.setis.desafiojava.dto.auth.TokenRequest;
import br.com.setis.desafiojava.dto.auth.TokenResponse;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import br.com.setis.desafiojava.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;

  @MockitoBean private TransacaoMapper transacaoMapper;

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http.csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers("/v1/auth/token").permitAll().anyRequest().authenticated());
      return http.build();
    }
  }

  @Test
  @DisplayName("Deve gerar token JWT com sucesso")
  void deveGerarTokenJWT() throws Exception {

    TokenRequest request = new TokenRequest("teste@teste.com", "123456");

    TokenResponse token =
        new TokenResponse("token123", 100, 100, "refreshToken", "tokenType", "scope");

    when(authService.gerarToken(request.email(), request.password())).thenReturn(token);

    mockMvc
        .perform(
            post("/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("token123"))
        .andExpect(jsonPath("$.refresh_token").value("refreshToken"))
        .andExpect(jsonPath("$.token_type").value("tokenType"))
        .andExpect(jsonPath("$.scope").value("scope"))
        .andExpect(jsonPath("$.refresh_expires_in").value(100))
        .andExpect(jsonPath("$.expires_in").value(100));
  }

  @Test
  @DisplayName("Deve retornar Unauthorized caso a senha não esteja certa")
  void deveRetornarUnauthorized() throws Exception {
    TokenRequest request = new TokenRequest("teste@teste.com", "senhaErrada");

    when(authService.gerarToken(request.email(), request.password()))
        .thenThrow(
            new org.springframework.security.authentication.BadCredentialsException(
                "Credenciais inválidas"));

    mockMvc
        .perform(
            post("/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Deve retornar Bad Request se email ou senha estiverem vazios")
  void deveRetornarErroDeValidacao() throws Exception {
    TokenRequest requestInvalido = new TokenRequest("", "");

    mockMvc
        .perform(
            post("/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar Bad Request se for um e-mail inválido")
  void deveRetornarErroDeValidacaoEmail() throws Exception {
    TokenRequest requestInvalido = new TokenRequest("teste.com", "123456");

    mockMvc
        .perform(
            post("/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
        .andExpect(status().isBadRequest());
  }
}
