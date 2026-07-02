package br.com.setis.desafiojava.service;

import br.com.setis.desafiojava.dto.auth.TokenResponse;

public interface AuthService {
  TokenResponse gerarToken(String email, String password);
}
