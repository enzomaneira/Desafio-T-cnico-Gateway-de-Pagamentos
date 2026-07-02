package br.com.setis.desafiojava.exception;

import br.com.setis.desafiojava.dto.exception.BadRequest;
import br.com.setis.desafiojava.dto.exception.ErroGenerico;
import br.com.setis.desafiojava.dto.exception.ErroTransacao;
import br.com.setis.desafiojava.mapper.TransacaoMapper;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final TransacaoMapper transacaoMapper;

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BadRequest> tratarErrosDeValidacao(MethodArgumentNotValidException ex) {

    List<BadRequest.Violacao> violacoes =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new BadRequest.Violacao(fieldError.getField(), fieldError.getDefaultMessage()))
            .collect(Collectors.toList());

    return ResponseEntity.badRequest().body(new BadRequest(violacoes));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErroGenerico> tratarErrosDeValidacao(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(new ErroGenerico(ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Void> tratarErroDeCredenciais() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @ExceptionHandler(FalhaComunicacaoException.class)
  public ResponseEntity<Object> handleFalhaComunicacaoException(FalhaComunicacaoException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErroTransacao(
                "Falha na comunicação", ex.getMessage(), transacaoMapper.toDto(ex.getTransacao())));
  }

  @ExceptionHandler(TransacaoRecusadaException.class)
  public ResponseEntity<Object> handleTransacaoRecusada(TransacaoRecusadaException ex) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(
            new ErroTransacao(
                "Transação Negada", ex.getMessage(), transacaoMapper.toDto(ex.getTransacao())));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErroGenerico> tratarViolacaoDeConstraint(ConstraintViolationException ex) {
    return ResponseEntity.badRequest().body(new ErroGenerico(ex.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErroGenerico> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest()
        .body(new ErroGenerico("Corpo da requisição inválido ou malformado"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErroGenerico> tratarErroInesperado(Exception ex) throws Exception {
    if (ex instanceof AccessDeniedException
        || ex instanceof ServletException
        || ex instanceof TypeMismatchException) {
      throw ex;
    }

    log.error("Erro interno inesperado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErroGenerico("Erro interno inesperado. Contate o suporte."));
  }
}
