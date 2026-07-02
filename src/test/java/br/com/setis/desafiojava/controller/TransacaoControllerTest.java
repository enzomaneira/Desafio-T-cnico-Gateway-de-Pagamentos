package br.com.setis.desafiojava.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.setis.desafiojava.domain.entity.*;
import br.com.setis.desafiojava.dto.pagamento.CriarTransacaoRequest;
import br.com.setis.desafiojava.dto.pagamento.DadosPixRequest;
import br.com.setis.desafiojava.dto.pagamento.ReembolsoResponse;
import br.com.setis.desafiojava.dto.pagamento.TransacaoResponse;
import br.com.setis.desafiojava.exception.FalhaComunicacaoException;
import br.com.setis.desafiojava.exception.TransacaoRecusadaException;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import br.com.setis.desafiojava.service.TransacaoService;
import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransacaoController.class)
@EnableMethodSecurity
public class TransacaoControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TransacaoService transacaoService;

  @MockitoBean private AuthenticationParseJwt authenticationParseJwt;

  @MockitoBean private TransacaoMapper transacaoMapper;

  @Test
  @DisplayName("Deve gerar uma transação com sucesso")
  void deveGerarTransacaoComSucesso() throws Exception {
    String lojistaId = UUID.randomUUID().toString();

    CriarTransacaoRequest request = criarRequestValido();

    TransacaoResponse transacaoResponse = criarResponseMock(request, lojistaId);

    when(transacaoService.criarTransacao(
            any(CriarTransacaoRequest.class), eq(lojistaId), eq("adm@paygo.com.br")))
        .thenReturn(transacaoResponse);
    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any(Jwt.class)))
        .thenReturn(Cargo.ADMINISTRADOR);
    when(authenticationParseJwt.obterUsername(any(Jwt.class))).thenReturn("adm@paygo.com.br");
    when(authenticationParseJwt.obterMerchantId(any(Jwt.class))).thenReturn(lojistaId);

    mockMvc
        .perform(
            post("/v1/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(StatusTransacao.AGUARDANDO_PAGAMENTO.toString()))
        .andExpect(jsonPath("$.id").value(transacaoResponse.id()))
        .andExpect(jsonPath("$.metodoPagamento").value(MetodoPagamento.PIX.toString()));

    verify(transacaoService).criarTransacao(any(), any(), any());
  }

  @Test
  @DisplayName("Deve gerar uma transação com sucesso usando o header X-On-Behalf-Of")
  void deveGerarTransacaoComSucessoComHeaderOnBehalf() throws Exception {
    String merchantIdToken = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.ADMINISTRADOR);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("admin@paygo.com.br");
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(merchantIdToken);

    CriarTransacaoRequest request = criarRequestValido();
    TransacaoResponse responseMock = criarResponseMock(request, xOnBehalfOf);

    when(transacaoService.criarTransacao(any(), eq(xOnBehalfOf), any())).thenReturn(responseMock);

    mockMvc
        .perform(
            post("/v1/transacoes")
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString(responseMock.id())));

    verify(transacaoService).criarTransacao(any(), eq(xOnBehalfOf), any());
  }

  @Test
  @DisplayName("Deve usar o id do prórpio token quando gerente, mesmo enviando X-On-Behalf-Of")
  void deveIgnorarHeaderOnBehalfOfSeNaoForAdmin() throws Exception {
    String merchantIdToken = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("loja@teste.com");
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(merchantIdToken);

    CriarTransacaoRequest request = criarRequestValido();
    TransacaoResponse responseMock = criarResponseMock(request, merchantIdToken);

    when(transacaoService.criarTransacao(any(), eq(merchantIdToken), any()))
        .thenReturn(responseMock);

    mockMvc
        .perform(
            post("/v1/transacoes")
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isCreated());

    verify(transacaoService).criarTransacao(any(), eq(merchantIdToken), any());
  }

  @Test
  @DisplayName("Deve retornar Bad Request quando o payload for inválido")
  void deveRetornarBadRequestPayloadInvalido() throws Exception {
    CriarTransacaoRequest requestInvalido = new CriarTransacaoRequest(null, null, null, null);

    mockMvc
        .perform(
            post("/v1/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(transacaoService);
  }

  @Test
  @DisplayName("Deve retornar Bad Request quando o serviço lançar exceção de negócio")
  void deveRetornarErroDeNegocio() throws Exception {
    UUID lojistaId = UUID.randomUUID();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId.toString());
    when(authenticationParseJwt.obterUsername(any())).thenReturn("user");

    CriarTransacaoRequest request = criarRequestValido();

    doThrow(new IllegalArgumentException("Lojista sem provedor configurado"))
        .when(transacaoService)
        .criarTransacao(any(), any(), any());

    mockMvc
        .perform(
            post("/v1/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isBadRequest());

    verify(transacaoService).criarTransacao(any(), any(), any());
  }

  @Test
  @DisplayName("Deve negar acesso a usuário não autenticado")
  void deveRetornarForbiddenSemRole() throws Exception {
    CriarTransacaoRequest request = criarRequestValido();

    mockMvc
        .perform(
            post("/v1/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUEBRADA"))))
        .andExpect(status().isForbidden());

    verifyNoInteractions(transacaoService);
  }

  @Test
  @DisplayName("Deve listar transações com filtros (Data, Status, Método)")
  void deveListarTransacoesComFiltros() throws Exception {
    String lojistaId = UUID.randomUUID().toString();
    LocalDate dataInicio = LocalDate.of(2026, 2, 1);
    LocalDate dataFim = LocalDate.of(2026, 2, 28);

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("gerente@loja.com");

    Page<TransacaoResponse> pagina = new PageImpl<>(List.of());

    when(transacaoService.listarTransacoes(
            any(Pageable.class),
            eq(lojistaId),
            eq(dataInicio),
            eq(dataFim),
            eq(StatusTransacao.CONFIRMADA),
            eq(MetodoPagamento.PIX)))
        .thenReturn(pagina);

    mockMvc
        .perform(
            get("/v1/transacoes")
                .param("dataInicio", "2026-02-01") // Formato ISO
                .param("dataFim", "2026-02-28")
                .param("status", "CONFIRMADA")
                .param("metodo", "PIX")
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conteudo").isArray())
        .andExpect(jsonPath("$.totalRegistros").value(0));

    verify(transacaoService)
        .listarTransacoes(
            any(Pageable.class),
            eq(lojistaId),
            eq(dataInicio),
            eq(dataFim),
            eq(StatusTransacao.CONFIRMADA),
            eq(MetodoPagamento.PIX));
  }

  @Test
  @DisplayName("Deve respeitar a paginação e ordenação padrão")
  void deveRespeitarPaginacao() throws Exception {
    String lojistaId = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);

    TransacaoResponse item = criarResponseMock(criarRequestValido(), lojistaId);
    Page<TransacaoResponse> paginaMock = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

    when(transacaoService.listarTransacoes(any(), any(), any(), any(), any(), any()))
        .thenReturn(paginaMock);

    mockMvc
        .perform(
            get("/v1/transacoes")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "dataCriacao,desc")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paginaAtual").value(1))
        .andExpect(jsonPath("$.itensPorPagina").value(10));

    verify(transacaoService)
        .listarTransacoes(
            argThat(
                p ->
                    p.getPageNumber() == 0
                        && p.getPageSize() == 10
                        && p.getSort().getOrderFor("dataCriacao") != null),
            eq(lojistaId),
            isNull(),
            isNull(),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName("Deve conseguir listar transações usando On-Behalf-Of, quando ADMINISTRADOR")
  void deveListarComoAdminOnBehalfOf() throws Exception {
    String idAdmin = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.ADMINISTRADOR);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(idAdmin);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("admin");

    Page<TransacaoResponse> paginaMock = new PageImpl<>(List.of());

    when(transacaoService.listarTransacoes(any(), eq(xOnBehalfOf), any(), any(), any(), any()))
        .thenReturn(paginaMock);

    mockMvc
        .perform(
            get("/v1/transacoes")
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk());

    verify(transacaoService).listarTransacoes(any(), eq(xOnBehalfOf), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Deve ignorar o header On-Behalf-Of quando usuário comum tentar usar")
  void deveIgnorarHeaderSeNaoForAdmin() throws Exception {
    String idLojista = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(idLojista);

    Page<TransacaoResponse> paginaMock = new PageImpl<>(List.of());

    when(transacaoService.listarTransacoes(any(), eq(idLojista), any(), any(), any(), any()))
        .thenReturn(paginaMock);

    mockMvc
        .perform(
            get("/v1/transacoes")
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isOk());

    verify(transacaoService).listarTransacoes(any(), eq(idLojista), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Deve consultar transação usando o ID no JWT, ignorando o header X-On-Behalf-Of")
  void deveConsultarTransacaoPeloIdDoToken() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaIdToken = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaIdToken);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("loja@teste.com");

    TransacaoResponse response = criarResponseMock(criarRequestValido(), lojistaIdToken);

    when(transacaoService.listarTransacaoPorId(eq(txId), eq(lojistaIdToken))).thenReturn(response);

    mockMvc
        .perform(
            get("/v1/transacoes/{txId}", txId)
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists());

    verify(transacaoService).listarTransacaoPorId(txId, lojistaIdToken);
  }

  @Test
  @DisplayName(
      "Deve consultar transação de outro lojista via Header quando admin usando X-On-Behalf-Of")
  void deveConsultarTransacaoComHeaderSendoAdmin() throws Exception {
    String txId = UUID.randomUUID().toString();
    String merchantIdToken = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.ADMINISTRADOR);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(merchantIdToken);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("admin@paygo.com.br");

    TransacaoResponse response = criarResponseMock(criarRequestValido(), xOnBehalfOf);

    when(transacaoService.listarTransacaoPorId(eq(txId), eq(xOnBehalfOf))).thenReturn(response);

    mockMvc
        .perform(
            get("/v1/transacoes/{txId}", txId)
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cnpjLojista").value(response.cnpjLojista()));

    verify(transacaoService).listarTransacaoPorId(txId, xOnBehalfOf);
  }

  @Test
  @DisplayName("Deve retornar Bad Request quando transação não for encontrada")
  void deveRetornarErroQuandoTransacaoNaoExiste() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaId = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("loja@teste.com");

    doThrow(new IllegalArgumentException("Transação não encontrada"))
        .when(transacaoService)
        .listarTransacaoPorId(eq(txId), eq(lojistaId));

    mockMvc
        .perform(
            get("/v1/transacoes/{txId}", txId)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve realizar estorno total com sucesso, quando não há query param")
  void deveRealizarEstornoTotal() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaId = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("gerente@loja.com");

    ReembolsoResponse response =
        new ReembolsoResponse(
            UUID.randomUUID().toString(),
            "R$ 100,00",
            StatusReembolso.CONCLUIDO,
            LocalDateTime.now());

    when(transacaoService.realizarEstorno(
            eq(txId), eq(lojistaId), isNull(), eq("gerente@loja.com")))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/v1/transacoes/{txId}/void", txId)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONCLUIDO"));
  }

  @Test
  @DisplayName("Deve realizar estorno parcial com valor informado via query param")
  void deveRealizarEstornoParcial() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaId = UUID.randomUUID().toString();
    String valorParcial = "1050";

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("gerente@loja.com");

    ReembolsoResponse response =
        new ReembolsoResponse(
            UUID.randomUUID().toString(),
            "R$ 10,50",
            StatusReembolso.CONCLUIDO,
            LocalDateTime.now());

    when(transacaoService.realizarEstorno(eq(txId), eq(lojistaId), eq(valorParcial), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/v1/transacoes/{txId}/void", txId)
                .param("amount", valorParcial)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.valorFormatado").value("R$ 10,50"));
  }

  @Test
  @DisplayName("Deve conseguir estornar usando o header X-On-Behfalf quando ADMINISTRADOR")
  void deveRealizarEstornoComoAdminOnBehalfOf() throws Exception {
    String txId = UUID.randomUUID().toString();
    String merchantIdToken = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.ADMINISTRADOR);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(merchantIdToken);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("admin@paygo.com.br");

    ReembolsoResponse response =
        new ReembolsoResponse(
            UUID.randomUUID().toString(),
            "R$ 50,00",
            StatusReembolso.CONCLUIDO,
            LocalDateTime.now());

    when(transacaoService.realizarEstorno(
            eq(txId), eq(xOnBehalfOf), isNull(), eq("admin@paygo.com.br")))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/v1/transacoes/{txId}/void", txId)
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Deve retornar Bad Request se o serviço rejeitar o estorno")
  void deveRetornarErroSeEstornoFalhar() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaId = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);

    doThrow(new IllegalArgumentException("Valor ultrapassa valor original"))
        .when(transacaoService)
        .realizarEstorno(any(), any(), any(), any());

    mockMvc
        .perform(
            post("/v1/transacoes/{txId}/void", txId)
                .param("amount", "999999")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve listar reembolsos de uma transação")
  void deveListarReembolsosPorTransacao() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaId = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("gerente@loja.com");

    ReembolsoResponse reembolso =
        new ReembolsoResponse(
            UUID.randomUUID().toString(),
            "R$ 50,00",
            StatusReembolso.CONCLUIDO,
            LocalDateTime.now());
    Page<ReembolsoResponse> pagina = new PageImpl<>(List.of(reembolso));

    when(transacaoService.listarReembolsoPorTransacao(any(Pageable.class), eq(txId), eq(lojistaId)))
        .thenReturn(pagina);

    mockMvc
        .perform(
            get("/v1/transacoes/{txId}/estornos", txId)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conteudo[0].valorFormatado").value("R$ 50,00"))
        .andExpect(jsonPath("$.totalRegistros").value(1));
  }

  @Test
  @DisplayName(
      "Deve listar reembolsos de outro lojista usando o header On-Behalf-Of, quando ADMINISTRADOR")
  void deveListarReembolsosComoAdminOnBehalfOf() throws Exception {
    String txId = UUID.randomUUID().toString();
    String adminId = UUID.randomUUID().toString();
    String xOnBehalfOf = UUID.randomUUID().toString(); // ID do Header

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.ADMINISTRADOR);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(adminId);
    when(authenticationParseJwt.obterUsername(any())).thenReturn("admin");

    Page<ReembolsoResponse> pagina = new PageImpl<>(List.of());

    when(transacaoService.listarReembolsoPorTransacao(
            any(Pageable.class), eq(txId), eq(xOnBehalfOf)))
        .thenReturn(pagina);

    mockMvc
        .perform(
            get("/v1/transacoes/{txId}/estornos", txId)
                .header("X-On-Behalf-Of", xOnBehalfOf)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Deve respeitar a paginação na listagem de reembolsos")
  void deveRespeitarPaginacaoReembolsos() throws Exception {
    String txId = UUID.randomUUID().toString();
    String lojistaId = UUID.randomUUID().toString();

    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(lojistaId);

    Page<ReembolsoResponse> pagina = new PageImpl<>(List.of());

    when(transacaoService.listarReembolsoPorTransacao(any(Pageable.class), any(), any()))
        .thenReturn(pagina);

    mockMvc
        .perform(
            get("/v1/transacoes/{txId}/estornos", txId)
                .param("page", "2")
                .param("size", "5")
                .param("sort", "valorQuantia,desc")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isOk());

    verify(transacaoService)
        .listarReembolsoPorTransacao(
            argThat(
                p ->
                    p.getPageNumber() == 1
                        && p.getPageSize() == 5
                        && p.getSort().getOrderFor("valorQuantia") != null),
            eq(txId),
            eq(lojistaId));
  }

  @Test
  @DisplayName("Deve capturar TransacaoRecusadaException e retornar 402 Payment Required")
  void deveTratarTransacaoRecusada() throws Exception {
    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(UUID.randomUUID().toString());
    when(authenticationParseJwt.obterUsername(any())).thenReturn("gerente@loja.com");

    CriarTransacaoRequest request = criarRequestValido();

    Transacao transacaoFake =
        Transacao.builder()
            .id(UUID.randomUUID())
            .valorQuantia(new BigDecimal("100.00"))
            .valorMoeda("BRL")
            .build();

    TransacaoRecusadaException erro =
        new TransacaoRecusadaException("Saldo insuficiente", transacaoFake);

    when(transacaoService.criarTransacao(any(), any(), any())).thenThrow(erro);

    mockMvc
        .perform(
            post("/v1/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.erro").value("Transação Negada"))
        .andExpect(jsonPath("$.motivo").value("Saldo insuficiente"));
  }

  @Test
  @DisplayName("Deve capturar FalhaComunicacaoException e retornar 500 Internal Server Error")
  void deveTratarFalhaComunicacao() throws Exception {
    when(authenticationParseJwt.obterCargoBaseadoEmRoles(any())).thenReturn(Cargo.GERENTE);
    when(authenticationParseJwt.obterMerchantId(any())).thenReturn(UUID.randomUUID().toString());
    when(authenticationParseJwt.obterUsername(any())).thenReturn("gerente@loja.com");

    CriarTransacaoRequest request = criarRequestValido();

    Transacao transacaoFake =
        Transacao.builder()
            .id(UUID.randomUUID())
            .valorQuantia(new BigDecimal("100.00"))
            .valorMoeda("BRL")
            .build();

    FalhaComunicacaoException erro =
        new FalhaComunicacaoException(
            "Timeout no Gateway", new RuntimeException("Timeout"), transacaoFake);

    when(transacaoService.criarTransacao(any(), any(), any())).thenThrow(erro);

    mockMvc
        .perform(
            post("/v1/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"))))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.erro").value("Falha na comunicação"))
        .andExpect(jsonPath("$.motivo").value("Timeout no Gateway"));
  }

  private CriarTransacaoRequest criarRequestValido() {
    DadosPixRequest pix =
        new DadosPixRequest(
            "77027447000189", // chave pix cnpj
            LocalDateTime.now(),
            Provedor.CIELO);
    return new CriarTransacaoRequest("100", "BRL", MetodoPagamento.PIX, pix);
  }

  private TransacaoResponse criarResponseMock(CriarTransacaoRequest req, String lojistaId) {
    return new TransacaoResponse(
        UUID.randomUUID().toString(),
        lojistaId,
        "solicitante",
        StatusTransacao.AGUARDANDO_PAGAMENTO,
        "Sucesso",
        "R$ 1,00",
        req.metodo(),
        null,
        LocalDateTime.now(),
        LocalDateTime.now());
  }
}
