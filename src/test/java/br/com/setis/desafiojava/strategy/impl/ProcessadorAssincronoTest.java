package br.com.setis.desafiojava.strategy.impl;

import static org.junit.jupiter.api.Assertions.*;

import br.com.setis.desafiojava.domain.entity.*;
import br.com.setis.desafiojava.dto.pagamento.DadosBoletoRequest;
import br.com.setis.desafiojava.dto.pagamento.DadosPixRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessadorAssincronoTest {
  @InjectMocks private ProcessarPix processarPix;

  @InjectMocks private ProcessarBoleto processarBoleto;

  private Transacao transacao;

  @BeforeEach
  void setup() {
    Lojista lojista = Lojista.builder().nomeFantasia("Empresas Inc.").build();
    transacao = new Transacao();
    transacao.setId(UUID.randomUUID());
    transacao.setLojista(lojista);
    transacao.setValor(Money.of(new BigDecimal("100.00"), "BRL"));
    transacao.setDadosPagamento(new DadosPagamento());
  }

  @Test
  @DisplayName("PIX: Deve gerar QR Code e mudar status para AGUARDANDO_PAGAMENTO")
  void deveIniciarProcessamentoPix() {
    DadosPixRequest request =
        new DadosPixRequest("chave@pix.com", LocalDateTime.now(), Provedor.CIELO);

    processarPix.processar(transacao, request);

    assertEquals(StatusTransacao.AGUARDANDO_PAGAMENTO, transacao.getStatus());
    assertEquals("chave@pix.com", transacao.getDadosPagamento().getChavePix());
    assertNotNull(transacao.getDadosPagamento().getPixQrCodeBase64());
    assertNotNull(transacao.getIdTransacaoPsp());
    assertEquals("201 - OK", transacao.getRespostaPspPura());
  }

  @Test
  @DisplayName("BOLETO: Deve gerar Linha Digitável e mudar status para AGUARDANDO_PAGAMENTO")
  void deveIniciarProcessamentoBoleto() {
    DadosBoletoRequest request =
        new DadosBoletoRequest(
            "422.157.160-88", "teste@teste.com", LocalDateTime.now().plusMonths(1), Provedor.ITAU);

    processarBoleto.processar(transacao, request);

    assertEquals(StatusTransacao.AGUARDANDO_PAGAMENTO, transacao.getStatus());
    assertNotNull(transacao.getDadosPagamento().getCodigoBarrasBoleto());
    assertEquals(request.dataVencimento(), transacao.getDadosPagamento().getDataExpiracao());
    assertEquals("201 - OK", transacao.getRespostaPspPura());
  }
}
