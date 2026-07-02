package br.com.setis.desafiojava.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.setis.desafiojava.domain.entity.*;
import br.com.setis.desafiojava.dto.pagamento.*;
import br.com.setis.desafiojava.mapper.ReembolsoMapper;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import br.com.setis.desafiojava.repository.LojistaRepository;
import br.com.setis.desafiojava.repository.ReembolsoRepository;
import br.com.setis.desafiojava.repository.TransacaoRepository;
import br.com.setis.desafiojava.service.impl.TransacaoServiceImpl;
import br.com.setis.desafiojava.strategy.ProcessadorPagamentoFactory;
import br.com.setis.desafiojava.strategy.ProcessadorPagamentoStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

  @InjectMocks private TransacaoServiceImpl transacaoService;

  @Mock private TransacaoRepository transacaoRepository;

  @Mock private TransacaoMapper transacaoMapper;

  @Mock private ReembolsoRepository reembolsoRepository;

  @Mock private ReembolsoMapper reembolsoMapper;

  @Mock private LojistaRepository lojistaRepository;

  @Mock private ProcessadorPagamentoFactory processadorFactory;

  @Mock private ProcessadorPagamentoStrategy processadorStrategy;

  @Captor private ArgumentCaptor<Transacao> transacaoCaptor;

  @Test
  @DisplayName("Deve criar transação com sucesso")
  void deveCriarTransacaoComSucesso() {
    String lojistaId = UUID.randomUUID().toString();
    CriarTransacaoRequest request = criarRequestPadrao(Provedor.CIELO);
    Lojista lojista = criarLojistaAtivo(UUID.fromString(lojistaId), Provedor.CIELO);

    when(lojistaRepository.findById(UUID.fromString(lojistaId))).thenReturn(Optional.of(lojista));
    when(transacaoRepository.saveAndFlush(any(Transacao.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(processadorFactory.get(MetodoPagamento.PIX)).thenReturn(processadorStrategy);
    doNothing().when(processadorStrategy).processar(any(), any());
    when(transacaoRepository.save(any(Transacao.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(transacaoMapper.toDto(any())).thenReturn(mock(TransacaoResponse.class));

    transacaoService.criarTransacao(request, lojistaId, "admin@paygo.com.br");

    verify(processadorStrategy, times(1)).processar(any(Transacao.class), any());

    verify(transacaoRepository, times(1)).saveAndFlush(any());
    verify(transacaoRepository, times(1)).save(transacaoCaptor.capture());

    Transacao transacaoSalva = transacaoCaptor.getValue();
    assertEquals(new BigDecimal("1.00"), transacaoSalva.getValorQuantia());
    assertEquals(StatusTransacao.TRANSACAO_INICIADA, transacaoSalva.getStatus());
  }

  @Test
  @DisplayName("Deve lançar execeção quando lojista não existe")
  void deveLancarErroLojistaInexistente() {
    String lojistaId = UUID.randomUUID().toString();
    CriarTransacaoRequest request = criarRequestPadrao(Provedor.CIELO);

    when(lojistaRepository.findById(UUID.fromString(lojistaId))).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> transacaoService.criarTransacao(request, lojistaId, "admin"));

    verifyNoInteractions(transacaoRepository);
  }

  @Test
  @DisplayName("Deve lançar execeção quando lojista está inativo")
  void deveLancarErroLojistaInativo() {
    String lojistaId = UUID.randomUUID().toString();
    Lojista lojistaInativo = Lojista.builder().id(UUID.fromString(lojistaId)).ativo(false).build();
    CriarTransacaoRequest request = criarRequestPadrao(Provedor.CIELO);

    when(lojistaRepository.findById(UUID.fromString(lojistaId)))
        .thenReturn(Optional.of(lojistaInativo));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> transacaoService.criarTransacao(request, lojistaId, "admin"));
    assertEquals("Lojista que busca criar pagamento está inativo", ex.getMessage());
  }

  @Test
  @DisplayName("Deve lançar execeção quando lojista não tem o provedor configurado")
  void deveLancarErroProvedorNaoConfigurado() {
    String lojistaId = UUID.randomUUID().toString();
    Lojista lojista = criarLojistaAtivo(UUID.fromString(lojistaId), Provedor.CIELO);
    CriarTransacaoRequest request = criarRequestPadrao(Provedor.REDE);

    when(lojistaRepository.findById(UUID.fromString(lojistaId))).thenReturn(Optional.of(lojista));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> transacaoService.criarTransacao(request, lojistaId, "admin"));
    assertEquals("O lojista não tem o provedor selecionado configurado", ex.getMessage());
  }

  @Test
  @DisplayName(
      "Deve salvar com status FALHA_COM_FORNECEDOR ao ocorrer erro inesperado no processamento")
  void deveTratarErroGenericoNoProcessamento() {
    String lojistaId = UUID.randomUUID().toString();
    Lojista lojista = criarLojistaAtivo(UUID.fromString(lojistaId), Provedor.CIELO);
    CriarTransacaoRequest request = criarRequestPadrao(Provedor.CIELO);

    when(lojistaRepository.findById(any())).thenReturn(Optional.of(lojista));
    when(transacaoRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    when(processadorFactory.get(any())).thenReturn(processadorStrategy);

    doThrow(new RuntimeException("Erro de conexão DB"))
        .when(processadorStrategy)
        .processar(any(), any());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> transacaoService.criarTransacao(request, lojistaId, "admin"));
    assertEquals("Erro de conexão DB", ex.getMessage());

    verify(transacaoRepository).save(transacaoCaptor.capture());
    Transacao transacaoFinal = transacaoCaptor.getValue();

    assertEquals(StatusTransacao.FALHA_COM_FORNECEDOR, transacaoFinal.getStatus());
  }

  @Test
  @DisplayName("Deve listar transações paginadas chamando o repositório corretamente")
  void deveListarTransacoes() {
    Pageable pageable = Pageable.unpaged();
    String lojistaId = UUID.randomUUID().toString();
    LocalDate dataInicio = LocalDate.now();

    Transacao transacao = new Transacao();
    Page<Transacao> pageEntity = new PageImpl<>(List.of(transacao));

    when(transacaoRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
        .thenReturn(pageEntity);

    when(transacaoMapper.toDto(any())).thenReturn(mock(TransacaoResponse.class));

    Page<TransacaoResponse> resultado =
        transacaoService.listarTransacoes(
            pageable, lojistaId, dataInicio, null, StatusTransacao.CONFIRMADA, null);

    assertNotNull(resultado);
    assertEquals(1, resultado.getTotalElements());
    verify(transacaoRepository)
        .findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
  }

  @Test
  @DisplayName("Deve buscar transação por ID com sucesso")
  void deveBuscarTransacaoPorId() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();
    Transacao transacao = new Transacao();

    when(transacaoRepository.findByIdAndLojista_Id(txId, lojistaId))
        .thenReturn(Optional.of(transacao));
    when(transacaoMapper.toDto(transacao)).thenReturn(mock(TransacaoResponse.class));

    TransacaoResponse response =
        transacaoService.listarTransacaoPorId(txId.toString(), lojistaId.toString());

    assertNotNull(response);
    verify(transacaoRepository).findByIdAndLojista_Id(txId, lojistaId);
  }

  @Test
  @DisplayName("Deve lançar erro ao buscar transação inexistente ou de outro lojista")
  void deveFalharBuscarTransacaoInexistente() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();

    when(transacaoRepository.findByIdAndLojista_Id(txId, lojistaId)).thenReturn(Optional.empty());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> transacaoService.listarTransacaoPorId(txId.toString(), lojistaId.toString()));

    assertEquals("Transação não encontrada", ex.getMessage());
  }

  @Test
  @DisplayName("Deve realizar estorno parcial com sucesso")
  void deveRealizarEstornoParcial() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();
    String valorSolicitado = "5000";

    Transacao transacaoOriginal =
        criarTransacaoConfirmada(txId, lojistaId, new BigDecimal("100.00"));

    when(transacaoRepository.findByIdWithLock(txId, lojistaId))
        .thenReturn(Optional.of(transacaoOriginal));
    when(reembolsoRepository.save(any(Reembolso.class))).thenAnswer(i -> i.getArgument(0));
    when(reembolsoMapper.toDto(any())).thenReturn(mock(ReembolsoResponse.class));

    transacaoService.realizarEstorno(
        txId.toString(), lojistaId.toString(), valorSolicitado, "solicitante");

    verify(transacaoRepository).save(transacaoCaptor.capture());
    Transacao transacaoAtualizada = transacaoCaptor.getValue();
    assertEquals(StatusTransacao.PARCIALMENTE_REEMBOLSADA, transacaoAtualizada.getStatus());

    ArgumentCaptor<Reembolso> reembolsoCaptor = ArgumentCaptor.forClass(Reembolso.class);
    verify(reembolsoRepository).save(reembolsoCaptor.capture());
    Reembolso reembolsoSalvo = reembolsoCaptor.getValue();

    assertEquals(0, new BigDecimal("50.00").compareTo(reembolsoSalvo.getValorQuantia()));
    assertEquals(StatusReembolso.CONCLUIDO, reembolsoSalvo.getStatus());
  }

  @Test
  @DisplayName("Deve realizar estorno do valor total, calculado automaticamente, com sucesso")
  void deveRealizarEstornoTotalAutomatico() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();
    String valorSolicitado = null;

    Transacao transacaoOriginal =
        criarTransacaoConfirmada(txId, lojistaId, new BigDecimal("100.00"));

    when(transacaoRepository.findByIdWithLock(txId, lojistaId))
        .thenReturn(Optional.of(transacaoOriginal));
    when(reembolsoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reembolsoMapper.toDto(any())).thenReturn(mock(ReembolsoResponse.class));

    transacaoService.realizarEstorno(
        txId.toString(), lojistaId.toString(), valorSolicitado, "solicitante");

    verify(transacaoRepository).save(transacaoCaptor.capture());
    Transacao transacaoAtualizada = transacaoCaptor.getValue();

    assertEquals(StatusTransacao.REEMBOLSADA, transacaoAtualizada.getStatus());
  }

  @Test
  @DisplayName("Deve impedir estorno se transação não estiver CONFIRMADA ou ESTORNADA_PARCIALMENTE")
  void deveImpedirEstornoStatusInvalido() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();

    Transacao transacao = criarTransacaoConfirmada(txId, lojistaId, new BigDecimal("100.00"));
    transacao.setStatus(StatusTransacao.AGUARDANDO_PAGAMENTO);

    when(transacaoRepository.findByIdWithLock(txId, lojistaId)).thenReturn(Optional.of(transacao));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                transacaoService.realizarEstorno(
                    txId.toString(), lojistaId.toString(), "10.00", "user"));
    assertEquals(
        "Apenas transações CONFIRMADAS ou PARCIALMENTE_REEMBOLSADA podem ser estornadas.",
        ex.getMessage());
  }

  @Test
  @DisplayName("Deve impedir estorno se saldo for insuficiente")
  void deveImpedirEstornoSemSaldo() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();

    Transacao transacao = criarTransacaoConfirmada(txId, lojistaId, new BigDecimal("100.00"));

    Reembolso reembolsoExistente =
        Reembolso.builder()
            .valorQuantia(new BigDecimal("90.00"))
            .status(StatusReembolso.CONCLUIDO)
            .build();
    transacao.getReembolsos().add(reembolsoExistente);

    when(transacaoRepository.findByIdWithLock(txId, lojistaId)).thenReturn(Optional.of(transacao));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                transacaoService.realizarEstorno(
                    txId.toString(), lojistaId.toString(), "2000", "user"));

    assertTrue(ex.getMessage().contains("Saldo insuficiente"));
    verify(reembolsoRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve criar transação com Cartão e mascarar número corretamente")
  void deveCriarTransacaoComCartao() {
    String lojistaId = UUID.randomUUID().toString();
    String cartaoValido = "49927398716";
    String validadeFutura =
        YearMonth.now().plusYears(1).format(DateTimeFormatter.ofPattern("MM/yy"));

    DadosCartaoRequest dadosCartao =
        new DadosCartaoRequest(
            cartaoValido, "Titular Teste", validadeFutura, "131", Provedor.CIELO);
    CriarTransacaoRequest request =
        new CriarTransacaoRequest("1000", "BRL", MetodoPagamento.CARTAO_CREDITO, dadosCartao);

    Lojista lojista = criarLojistaAtivo(UUID.fromString(lojistaId), Provedor.CIELO);

    when(lojistaRepository.findById(UUID.fromString(lojistaId))).thenReturn(Optional.of(lojista));
    when(transacaoRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    when(processadorFactory.get(MetodoPagamento.CARTAO_CREDITO)).thenReturn(processadorStrategy);
    doNothing().when(processadorStrategy).processar(any(), any());
    when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(transacaoMapper.toDto(any())).thenReturn(mock(TransacaoResponse.class));

    transacaoService.criarTransacao(request, lojistaId, "admin");

    verify(transacaoRepository).save(transacaoCaptor.capture());
    Transacao transacaoSalva = transacaoCaptor.getValue();

    assertEquals(
        "**** **** **** 8716", transacaoSalva.getDadosPagamento().getNumeroCartaoMascarado());
    assertEquals("Titular Teste", transacaoSalva.getDadosPagamento().getNomeTitular());
  }

  @Test
  @DisplayName("Deve criar transação com Boleto e salvar dados corretamente")
  void deveCriarTransacaoComBoleto() {
    String lojistaId = UUID.randomUUID().toString();
    LocalDateTime dataVencimento = LocalDateTime.now().plusMonths(1);

    DadosBoletoRequest dadosBoleto =
        new DadosBoletoRequest("422.157.160-88", "teste@teste.com", dataVencimento, Provedor.ITAU);
    CriarTransacaoRequest request =
        new CriarTransacaoRequest("5000", "BRL", MetodoPagamento.BOLETO, dadosBoleto);

    Lojista lojista = criarLojistaAtivo(UUID.fromString(lojistaId), Provedor.ITAU);

    when(lojistaRepository.findById(UUID.fromString(lojistaId))).thenReturn(Optional.of(lojista));
    when(transacaoRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    when(processadorFactory.get(MetodoPagamento.BOLETO)).thenReturn(processadorStrategy);
    doNothing().when(processadorStrategy).processar(any(), any());
    when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(transacaoMapper.toDto(any())).thenReturn(mock(TransacaoResponse.class));

    transacaoService.criarTransacao(request, lojistaId, "admin");

    verify(transacaoRepository).save(transacaoCaptor.capture());
    Transacao transacaoSalva = transacaoCaptor.getValue();

    assertEquals(dataVencimento, transacaoSalva.getDadosPagamento().getDataExpiracao());
    assertEquals(Provedor.ITAU, transacaoSalva.getDadosPagamento().getProvedor());
  }

  @Test
  @DisplayName("Deve falhar estorno se valor for zero ou negativo")
  void deveFalharEstornoValorZero() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();

    Transacao transacao = criarTransacaoConfirmada(txId, lojistaId, new BigDecimal("100.00"));

    when(transacaoRepository.findByIdWithLock(txId, lojistaId)).thenReturn(Optional.of(transacao));

    String valorZero = "0";
    String valorNegativo = "-100";

    IllegalArgumentException exZero =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                transacaoService.realizarEstorno(
                    txId.toString(), lojistaId.toString(), valorZero, "user"));
    assertEquals("O valor do estorno deve ser maior que zero.", exZero.getMessage());

    IllegalArgumentException exNeg =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                transacaoService.realizarEstorno(
                    txId.toString(), lojistaId.toString(), valorNegativo, "user"));
    assertEquals("O valor do estorno deve ser maior que zero.", exNeg.getMessage());

    verify(transacaoRepository, times(2)).findByIdWithLock(txId, lojistaId);
  }

  @Test
  @DisplayName("Deve falhar estorno se transação não for encontrada (404)")
  void deveFalharEstornoNaoEncontrado() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();

    when(transacaoRepository.findByIdWithLock(txId, lojistaId)).thenReturn(Optional.empty());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                transacaoService.realizarEstorno(
                    txId.toString(), lojistaId.toString(), "1000", "user"));

    assertEquals("Transação não encontrada", ex.getMessage());
  }

  @Test
  @DisplayName("Deve listar reembolsos por transação paginados")
  void deveListarReembolsosPorTransacao() {
    UUID txId = UUID.randomUUID();
    UUID lojistaId = UUID.randomUUID();
    Pageable pageable = Pageable.unpaged();

    Reembolso reembolso = new Reembolso();
    Page<Reembolso> paginaEntity = new PageImpl<>(List.of(reembolso));

    when(reembolsoRepository.findAllByTxAndLojista_Id(pageable, txId, lojistaId))
        .thenReturn(paginaEntity);

    when(reembolsoMapper.toDto(reembolso)).thenReturn(mock(ReembolsoResponse.class));

    Page<ReembolsoResponse> resultado =
        transacaoService.listarReembolsoPorTransacao(
            pageable, txId.toString(), lojistaId.toString());

    assertNotNull(resultado);
    assertEquals(1, resultado.getTotalElements());
    verify(reembolsoRepository).findAllByTxAndLojista_Id(pageable, txId, lojistaId);
  }

  private Lojista criarLojistaAtivo(UUID id, Provedor provedorPermitido) {
    return Lojista.builder()
        .id(id)
        .cnpj("12345678000199")
        .ativo(true)
        .provedores(Set.of(provedorPermitido))
        .build();
  }

  private CriarTransacaoRequest criarRequestPadrao(Provedor provedor) {
    var dadosPix = new DadosPixRequest("chave-pix", LocalDateTime.now(), provedor);

    return new CriarTransacaoRequest("100", "BRL", MetodoPagamento.PIX, dadosPix);
  }

  private Transacao criarTransacaoConfirmada(UUID id, UUID lojistaId, BigDecimal valor) {
    return Transacao.builder()
        .id(id)
        .lojista(Lojista.builder().id(lojistaId).build())
        .valorQuantia(valor)
        .valorMoeda("BRL")
        .status(StatusTransacao.CONFIRMADA)
        .reembolsos(new java.util.ArrayList<>())
        .build();
  }
}
