package br.com.setis.desafiojava.service;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.dto.pagamento.CriarTransacaoRequest;
import br.com.setis.desafiojava.dto.pagamento.ReembolsoResponse;
import br.com.setis.desafiojava.dto.pagamento.TransacaoResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransacaoService {

  TransacaoResponse criarTransacao(
      CriarTransacaoRequest request, String lojistaId, String solicitante);

  Page<TransacaoResponse> listarTransacoes(
      Pageable pageable,
      String lojistaId,
      LocalDate dataInicio,
      LocalDate dataFim,
      StatusTransacao status,
      MetodoPagamento metodo);

  TransacaoResponse listarTransacaoPorId(String txId, String lojistaId);

  ReembolsoResponse realizarEstorno(
      String txId, String lojistaId, String valorSolicitado, String solicitante);

  Page<ReembolsoResponse> listarReembolsoPorTransacao(
      Pageable pageable, String txId, String lojistaId);
}
