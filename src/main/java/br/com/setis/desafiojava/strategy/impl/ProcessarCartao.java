package br.com.setis.desafiojava.strategy.impl;

import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.dto.pagamento.DadosCartaoRequest;
import br.com.setis.desafiojava.dto.pagamento.DadosPagamentoRequest;
import br.com.setis.desafiojava.exception.TransacaoRecusadaException;
import br.com.setis.desafiojava.strategy.ProcessadorDePagamentos;
import br.com.setis.desafiojava.strategy.ProcessadorPagamentoStrategy;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public abstract class ProcessarCartao extends ProcessadorDePagamentos
    implements ProcessadorPagamentoStrategy {
  public void processar(Transacao transacao, DadosPagamentoRequest request) {
    DadosCartaoRequest cartao = (DadosCartaoRequest) request;

    validarCartao(cartao, transacao);

    transacao.setIdTransacaoPsp(transacao.getId().toString().replaceAll("-", ""));
    super.simularProcessamentoExterno(transacao);
  }

  private void validarCartao(DadosCartaoRequest cartao, Transacao transacao) {
    if (!LuhnValido(cartao.numero())) {
      transacao.setStatus(StatusTransacao.NEGADA);
      transacao.setRespostaPspPura("Número de cartão inválido (Falha no Checksum)");
      throw new TransacaoRecusadaException(
          "Número de cartão inválido (Falha no Checksum)", transacao);
    }

    if (!dataValidadeValida(cartao.validade())) {
      transacao.setStatus(StatusTransacao.NEGADA);
      transacao.setRespostaPspPura("Cartão vencido ou data inválida");
      throw new TransacaoRecusadaException("Cartão vencido ou data inválida", transacao);
    }

    if (!isCvvValidoSimulado(cartao.cvv())) {
      transacao.setStatus(StatusTransacao.NEGADA);
      transacao.setRespostaPspPura("CVV Inválido");
      throw new TransacaoRecusadaException("CVV Inválido", transacao);
    }
  }

  private boolean LuhnValido(String numeroCartao) {
    String san = numeroCartao.replaceAll("\\D", "");

    int soma = 0;
    boolean dobrar = false;

    for (int i = san.length() - 1; i >= 0; i--) {
      int digito = Integer.parseInt(san.substring(i, i + 1));

      if (dobrar) {
        digito *= 2;
        if (digito > 9) {
          digito -= 9;
        }
      }

      soma += digito;
      dobrar = !dobrar;
    }

    return (soma % 10 == 0);
  }

  private boolean dataValidadeValida(String validade) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
      YearMonth dataValidade = YearMonth.parse(validade, formatter);
      YearMonth agora = YearMonth.now();

      return dataValidade.isAfter(agora) || dataValidade.equals(agora);
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private boolean isCvvValidoSimulado(String cvv) {
    if (cvv == null || !cvv.matches("\\d{3,4}")) {
      return false;
    }

    // Se o usuário mandar "999", fingimos que o banco recusou
    return !"999".equals(cvv);
  }
}
