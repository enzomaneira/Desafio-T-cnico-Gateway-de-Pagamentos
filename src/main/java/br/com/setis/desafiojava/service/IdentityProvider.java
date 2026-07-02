package br.com.setis.desafiojava.service;

import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import java.util.UUID;

public interface IdentityProvider {
  /**
   * Cria usuário no sistema de identidade externo (Keycloak)
   *
   * @return ID do usuário criado
   */
  String cadastrarUsuario(CriarUsuarioLojistaRequest usuarioLojista, UUID idLojista);
}
