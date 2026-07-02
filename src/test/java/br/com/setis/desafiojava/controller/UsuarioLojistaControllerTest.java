package br.com.setis.desafiojava.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import br.com.setis.desafiojava.infra.security.LojistaSecurity;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import br.com.setis.desafiojava.service.LojistaService;
import br.com.setis.desafiojava.service.UsuarioLojistaService;
import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LojistaController.class)
@EnableMethodSecurity
class UsuarioLojistaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UsuarioLojistaService usuarioLojistaService;

  @MockitoBean private LojistaService lojistaService;

  @MockitoBean private AuthenticationParseJwt authenticationParseJwt;

  @MockitoBean private TransacaoMapper transacaoMapper;

  @MockitoBean(name = "lojistaSecurity")
  private LojistaSecurity lojistaSecurity;

  @Test
  @DisplayName("Deve criar usuário com sucesso")
  void deveCriarUsuario() throws Exception {
    UUID idLojista = UUID.randomUUID();
    UUID idUsuario = UUID.randomUUID();

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Hanabi", "teste@teste.com", "123456", Cargo.GERENTE);

    UsuarioLojistaDto responseDto =
        new UsuarioLojistaDto(
            idUsuario,
            "Hanabi",
            "teste@teste.com",
            Cargo.GERENTE,
            true,
            idLojista,
            null,
            LocalDateTime.now(),
            LocalDateTime.now());

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    when(usuarioLojistaService.cadastrarUsuarioLojista(any(), any(), any()))
        .thenReturn(responseDto);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/usuarios", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(idUsuario.toString()))
        .andExpect(jsonPath("$.email").value("teste@teste.com"));
  }

  @Test
  @DisplayName("Deve retornar 403 Forbidden quando usuário não tem permissão")
  void deveRetornarForbiddenSeSemPermissao() throws Exception {
    UUID idLojista = UUID.randomUUID();
    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Hanabi", "teste@teste.com", "123", Cargo.GERENTE);

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(false);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/usuarios", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request se payload for inválido")
  void deveRetornarErroDeValidacao() throws Exception {
    UUID idLojista = UUID.randomUUID();
    var requestInvalido = new CriarUsuarioLojistaRequest("", "", "", null);

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/usuarios", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido))
                .with(csrf())
                .with(jwt()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve listar usuários de forma paginada")
  void deveListarUsuariosPaginados() throws Exception {
    UUID idLojista = UUID.randomUUID();

    UsuarioLojistaDto dto =
        new UsuarioLojistaDto(
            UUID.randomUUID(),
            "Hanabi",
            "teste@teste.com",
            Cargo.GERENTE,
            true,
            idLojista,
            null,
            LocalDateTime.now(),
            LocalDateTime.now());
    org.springframework.data.domain.Page<UsuarioLojistaDto> paginaMock =
        new org.springframework.data.domain.PageImpl<>(java.util.List.of(dto));

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    when(usuarioLojistaService.listarUsuariosPorLojista(any(), any())).thenReturn(paginaMock);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/v1/lojistas/{idLojista}/usuarios", idLojista)
                .param("page", "0")
                .param("size", "10")
                .with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conteudo[0].nome").value("Hanabi"))
        .andExpect(jsonPath("$.totalRegistros").value(1));
  }

  @Test
  @DisplayName("Deve retornar 400 bad request quando o email do usuário já existe")
  void deveRetornarErroSeEmailUsuarioDuplicado() throws Exception {
    UUID idLojista = UUID.randomUUID();

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Hanabi", "duplicado@teste.com", "123456", Cargo.GERENTE);

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    when(usuarioLojistaService.cadastrarUsuarioLojista(any(), eq(idLojista), any()))
        .thenThrow(new IllegalArgumentException("E-mail já cadastrado no sistema"));

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/usuarios", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }
}
