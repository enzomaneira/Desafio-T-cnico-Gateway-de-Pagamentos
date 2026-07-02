package br.com.setis.desafiojava.controller;

import br.com.setis.desafiojava.dto.auth.TokenRequest;
import br.com.setis.desafiojava.dto.auth.TokenResponse;
import br.com.setis.desafiojava.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${rotas.auth}")
public class AuthController {

  private final AuthService authService;

  @PostMapping(value = "/token")
  public ResponseEntity<TokenResponse> gerarToken(@RequestBody @Valid TokenRequest tokenRequest) {
    TokenResponse token = authService.gerarToken(tokenRequest.email(), tokenRequest.password());

    return ResponseEntity.ok(token);
  }
}
