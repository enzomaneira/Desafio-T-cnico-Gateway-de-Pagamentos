package br.com.setis.desafiojava.controller;

import static br.com.setis.desafiojava.domain.entity.Cargo.CARGOS_PERMITIDOS_ON_BEHALF;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.dto.pagamento.CriarTransacaoRequest;
import br.com.setis.desafiojava.dto.pagamento.ReembolsoResponse;
import br.com.setis.desafiojava.dto.pagamento.TransacaoResponse;
import br.com.setis.desafiojava.dto.pagina.PaginaResponse;
import br.com.setis.desafiojava.service.TransacaoService;
import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${rotas.transacao}")
public class TransacaoController {

  private final TransacaoService transacaoService;
  private final AuthenticationParseJwt authenticationParseJwt;

  @Value("${rotas.transacao}")
  private String rotaTransacao;

  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRADOR', 'SUPORTE')")
  public ResponseEntity<TransacaoResponse> criarTransacao(
      @RequestBody @Valid CriarTransacaoRequest request,
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = "X-On-Behalf-Of", required = false) String xOnBehalfOf,
      UriComponentsBuilder uriBuilder) {

    String solicitante = authenticationParseJwt.obterUsername(jwt);
    String lojistaId = decidirLojistaId(xOnBehalfOf, jwt);

    TransacaoResponse novaTransacao =
        transacaoService.criarTransacao(request, lojistaId, solicitante);
    URI uri = uriBuilder.path(rotaTransacao + "/{id}").buildAndExpand(novaTransacao.id()).toUri();

    return ResponseEntity.created(uri).body(novaTransacao);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PaginaResponse<TransacaoResponse>> listarTransacoes(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = "X-On-Behalf-Of", required = false) String xOnBehalfOf,
      @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim,
      @RequestParam(required = false) StatusTransacao status,
      @RequestParam(required = false) MetodoPagamento metodo) {
    String lojistaId = decidirLojistaId(xOnBehalfOf, jwt);

    Page<TransacaoResponse> page =
        transacaoService.listarTransacoes(pageable, lojistaId, dataInicio, dataFim, status, metodo);
    return ResponseEntity.ok(new PaginaResponse<>(page));
  }

  @GetMapping("/{txId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TransacaoResponse> listarTransacaoPorId(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = "X-On-Behalf-Of", required = false) String xOnBehalfOf,
      @PathVariable("txId") String txId) {
    String lojistaId = decidirLojistaId(xOnBehalfOf, jwt);

    return ResponseEntity.ok(transacaoService.listarTransacaoPorId(txId, lojistaId));
  }

  @PostMapping("/{txId}/void")
  @PreAuthorize("hasAnyRole('GERENTE', 'ADMINISTRADOR', 'SUPORTE')")
  public ResponseEntity<ReembolsoResponse> realizarEstorno(
      @PathVariable String txId,
      @RequestParam(value = "amount", required = false)
          @Pattern(regexp = "^\\d+$", message = "O valor deve ser informado sem pontos ou vírgulas")
          String amount,
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = "X-On-Behalf-Of", required = false) String xOnBehalfOf) {

    String solicitante = authenticationParseJwt.obterUsername(jwt);
    String lojistaId = decidirLojistaId(xOnBehalfOf, jwt);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(transacaoService.realizarEstorno(txId, lojistaId, amount, solicitante));
  }

  @GetMapping("/{txId}/estornos")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PaginaResponse<ReembolsoResponse>> listarReembolsoPorTransacao(
      @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
      @PathVariable String txId,
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = "X-On-Behalf-Of", required = false) String xOnBehalfOf) {
    String lojistaId = decidirLojistaId(xOnBehalfOf, jwt);

    Page<ReembolsoResponse> page =
        transacaoService.listarReembolsoPorTransacao(pageable, txId, lojistaId);
    return ResponseEntity.ok(new PaginaResponse<>(page));
  }

  private String decidirLojistaId(String xOnBehalfOf, Jwt jwt) {
    var cargo = authenticationParseJwt.obterCargoBaseadoEmRoles(jwt);
    String lojistaId;

    if (CARGOS_PERMITIDOS_ON_BEHALF.contains(cargo) && xOnBehalfOf != null) {
      lojistaId = xOnBehalfOf;
    } else {
      lojistaId = authenticationParseJwt.obterMerchantId(jwt);
    }

    if (lojistaId == null) {
      throw new IllegalArgumentException(
          "Não foi possível identificar o lojista: token sem merchant_id e sem X-On-Behalf-Of");
    }

    return lojistaId;
  }
}
