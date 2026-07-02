package br.com.setis.desafiojava.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.domain.entity.Provedor;
import br.com.setis.desafiojava.dto.lojista.CriarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.EditarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.ProvedoresRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaInicial;
import br.com.setis.desafiojava.dto.usuarioLojista.EditarUsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import br.com.setis.desafiojava.infra.security.LojistaSecurity;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import br.com.setis.desafiojava.service.LojistaService;
import br.com.setis.desafiojava.service.UsuarioLojistaService;
import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LojistaController.class)
@EnableMethodSecurity
public class LojistaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UsuarioLojistaService usuarioLojistaService;

  @MockitoBean private LojistaService lojistaService;

  @MockitoBean private AuthenticationParseJwt authenticationParseJwt;

  @MockitoBean private TransacaoMapper transacaoMapper;

  @MockitoBean(name = "lojistaSecurity")
  private LojistaSecurity lojistaSecurity;

  @Test
  @DisplayName("Deve cadastrar um novo Lojista com sucesso")
  void deveCadastrarLojista() throws Exception {
    CriarUsuarioLojistaInicial usuarioInicial =
        new CriarUsuarioLojistaInicial("Hanabi", "teste@teste.com", "123456");
    CriarLojistaRequest request =
        new CriarLojistaRequest("02.028.494/0001-48", "Empresas Inc", usuarioInicial);

    Lojista novoLojista =
        Lojista.builder().cnpj("02.028.494/0001-48").nomeFantasia("Empresas Inc").build();

    when(lojistaService.cadastrarLojista(any())).thenReturn(novoLojista);

    mockMvc
        .perform(
            post("/v1/lojistas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nomeFantasia").value("Empresas Inc"))
        .andExpect(jsonPath("$.cnpj").value("02.028.494/0001-48"));
  }

  @Test
  @DisplayName("Deve buscar Lojista por ID com sucesso")
  void deveBuscarLojistaPorId() throws Exception {
    UUID idLojista = UUID.randomUUID();

    Lojista lojista =
        Lojista.builder()
            .id(idLojista)
            .cnpj("02.028.494/0001-48")
            .nomeFantasia("Empresas Inc")
            .build();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    when(lojistaService.listarLojistaPorId(idLojista.toString()))
        .thenReturn(Optional.ofNullable(lojista));

    mockMvc
        .perform(get("/v1/lojistas/{id}", idLojista).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(idLojista.toString()))
        .andExpect(jsonPath("$.nomeFantasia").value("Empresas Inc"));
  }

  @Test
  @DisplayName("Deve retornar 204 No Content quando o lojista não existe")
  void deveRetornarNoContentLojistaInexistente() throws Exception {
    UUID idLojista = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    when(lojistaService.listarLojistaPorId(idLojista.toString()))
        .thenReturn(java.util.Optional.empty());

    mockMvc
        .perform(get("/v1/lojistas/{id}", idLojista).with(jwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request para CNPJ inválido")
  void deveFalharCnpjInvalido() throws Exception {
    CriarUsuarioLojistaInicial usuarioInicial =
        new CriarUsuarioLojistaInicial("Hanabi", "teste@teste.com", "123456");

    CriarLojistaRequest requestInvalido = new CriarLojistaRequest("Nome", "Razao", usuarioInicial);

    mockMvc
        .perform(
            post("/v1/lojistas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 se o CNPJ já estiver cadastrado")
  void deveRetornarErroSeCnpjDuplicado() throws Exception {
    CriarUsuarioLojistaInicial usuarioInicial =
        new CriarUsuarioLojistaInicial("Hanabi", "teste@teste.com", "123");
    CriarLojistaRequest request =
        new CriarLojistaRequest("02.028.494/0001-48", "Empresas Inc", usuarioInicial);

    when(lojistaService.cadastrarLojista(any()))
        .thenThrow(new IllegalArgumentException("Lojista já cadastrado com este CNPJ"));

    mockMvc
        .perform(
            post("/v1/lojistas")
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

  @Test
  @DisplayName("Deve retornar 200 ao adicionar novos provedores ao lojista")
  void deveRetornarOkAoAdicionarProvedores() throws Exception {
    UUID idLojista = UUID.randomUUID();
    ProvedoresRequest request =
        new ProvedoresRequest(Set.of(Provedor.GETNET, Provedor.SICOOB, Provedor.STONE));
    Lojista lojista =
        Lojista.builder()
            .id(idLojista)
            .nomeFantasia("Empresas INC")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .provedores(request.provedores())
            .build();

    when(lojistaService.cadastrarNovosProvedores(idLojista.toString(), request))
        .thenReturn(lojista);
    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(idLojista.toString()))
        .andExpect(jsonPath("$.provedores", containsInAnyOrder("GETNET", "SICOOB", "STONE")));
  }

  @Test
  @DisplayName("Deve retornar 400 caso o Id passado não exista ao adicionar provedores")
  void deveRetornarErroCasoIdPassadoNaoExista() throws Exception {
    UUID idLojista = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);
    when(lojistaService.cadastrarNovosProvedores(
            idLojista.toString(), new ProvedoresRequest(Set.of(Provedor.C6BANK))))
        .thenThrow(new IllegalArgumentException("Lojista não encontrado, verificar ID passado"));

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of()))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve remover retornar 200 ao remover provedores do lojista")
  void deveRemoverProvedores() throws Exception {
    UUID idLojista = UUID.randomUUID();

    ProvedoresRequest request = new ProvedoresRequest(Set.of(Provedor.GETNET));
    ProvedoresRequest provedoresARemover = new ProvedoresRequest(Set.of(Provedor.C6BANK));
    Lojista lojista =
        Lojista.builder()
            .id(idLojista)
            .nomeFantasia("Empresas INC")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .provedores(request.provedores())
            .build();

    when(lojistaService.removerProvedores(idLojista.toString(), provedoresARemover))
        .thenReturn(lojista);
    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores/remover", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(provedoresARemover))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(idLojista.toString()))
        .andExpect(jsonPath("$.provedores", containsInAnyOrder("GETNET")));
  }

  @Test
  @DisplayName("Deve retornar 400 caso o Id passado não exista ao remover provedores")
  void deveRetornarErroCasoIdPassadoNaoExistaAoRemoverProvedor() throws Exception {
    UUID idLojista = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);
    when(lojistaService.removerProvedores(
            idLojista.toString(), new ProvedoresRequest(Set.of(Provedor.C6BANK))))
        .thenThrow(new IllegalArgumentException("Lojista não encontrado, verificar ID passado"));

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores/remover", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of(Provedor.C6BANK)))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 caso o Id passado tenha lista vazia de provedores")
  void deveRetornarErroCasoListaVaziaAoRemoverProvedor() throws Exception {
    UUID idLojista = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);
    when(lojistaService.removerProvedores(
            idLojista.toString(), new ProvedoresRequest(Set.of(Provedor.C6BANK))))
        .thenThrow(new IllegalArgumentException("Lojista não possuí nenhum provedor casdastrado"));

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores/remover", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of(Provedor.C6BANK)))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 caso caso provedores seja lista vazia ao remover provedores")
  void deveRetornarErroCasoEnvieListaVaziaAoRemoverProvedor() throws Exception {
    UUID idLojista = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores/remover", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of()))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 caso caso provedores seja lista vazia ao adicionar provedores")
  void deveRetornarErroCasoEnvieListaVaziaAoAdicionarProvedor() throws Exception {
    UUID idLojista = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);

    mockMvc
        .perform(
            post("/v1/lojistas/{idLojista}/provedores", idLojista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of()))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve listar lojistas de forma paginada se for ADMINISTRADOR")
  void deveListarLojistasPaginados() throws Exception {
    Lojista lojista =
        Lojista.builder()
            .id(UUID.randomUUID())
            .nomeFantasia("Loja Listada")
            .cnpj("00.000.000/0001-00")
            .build();

    Page<Lojista> page = new PageImpl<>(List.of(lojista));

    when(lojistaService.listarLojistas(any(Pageable.class))).thenReturn(page);

    mockMvc
        .perform(
            get("/v1/lojistas")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conteudo[0].nomeFantasia").value("Loja Listada"))
        .andExpect(jsonPath("$.totalRegistros").value(1));
  }

  @Test
  @DisplayName("Deve atualizar lojista com sucesso se for ADMINISTRADOR")
  void deveAtualizarLojista() throws Exception {
    UUID id = UUID.randomUUID();
    EditarLojistaRequest request =
        new EditarLojistaRequest("22.377.499/0001-93", "Nome Atualizado", true);

    Lojista lojistaAtualizado =
        Lojista.builder()
            .id(id)
            .nomeFantasia("Nome Atualizado")
            .cnpj("22.377.499/0001-93")
            .ativo(true)
            .build();

    when(lojistaService.atualizarPorId(eq(id.toString()), any(EditarLojistaRequest.class)))
        .thenReturn(lojistaAtualizado);

    mockMvc
        .perform(
            put("/v1/lojistas/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nomeFantasia").value("Nome Atualizado"))
        .andExpect(jsonPath("$.cnpj").value("22.377.499/0001-93"));
  }

  @Test
  @DisplayName("Deve buscar usuário de lojista por ID com sucesso")
  void deveBuscarUsuarioPorLojista() throws Exception {
    UUID idLojista = UUID.randomUUID();
    UUID idUsuario = UUID.randomUUID();

    UsuarioLojistaDto dto =
        new UsuarioLojistaDto(
            idUsuario,
            "Gerente Loja",
            "gerente@loja.com",
            Cargo.GERENTE,
            true,
            idLojista,
            null,
            null,
            null);

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);
    when(usuarioLojistaService.listarUsuarioPorLojista(idUsuario.toString(), idLojista.toString()))
        .thenReturn(Optional.of(dto));

    mockMvc
        .perform(
            get("/v1/lojistas/{lojistaId}/usuarios/{usuarioId}", idLojista, idUsuario)
                .with(jwt())) // Token genérico, validado pelo mock do lojistaSecurity
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Gerente Loja"))
        .andExpect(jsonPath("$.email").value("gerente@loja.com"));
  }

  @Test
  @DisplayName("Deve retornar 204 No Content quando usuário do lojista não existe")
  void deveRetornarNoContentUsuarioInexistente() throws Exception {
    UUID idLojista = UUID.randomUUID();
    UUID idUsuario = UUID.randomUUID();

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);
    when(usuarioLojistaService.listarUsuarioPorLojista(any(), any())).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/v1/lojistas/{lojistaId}/usuarios/{usuarioId}", idLojista, idUsuario).with(jwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Deve atualizar usuário do lojista com sucesso")
  void deveAtualizarUsuarioLojista() throws Exception {
    UUID idLojista = UUID.randomUUID();
    UUID idUsuario = UUID.randomUUID();

    EditarUsuarioLojista request = new EditarUsuarioLojista("Analista Novo", Cargo.ANALISTA, false);

    UsuarioLojistaDto respostaMock =
        new UsuarioLojistaDto(
            idUsuario,
            "Analista Novo",
            "email@loja.com",
            Cargo.ANALISTA,
            false,
            idLojista,
            null,
            null,
            null);

    when(lojistaSecurity.podeGerenciarLojista(any(), any())).thenReturn(true);
    when(usuarioLojistaService.atualizarUsuarioPorId(
            eq(idUsuario.toString()), eq(idLojista.toString()), any(EditarUsuarioLojista.class)))
        .thenReturn(respostaMock);

    mockMvc
        .perform(
            put("/v1/lojistas/{lojistaId}/usuarios/{usuarioId}", idLojista, idUsuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Analista Novo"))
        .andExpect(jsonPath("$.cargo").value("ANALISTA"))
        .andExpect(jsonPath("$.ativo").value(false));
  }
}
