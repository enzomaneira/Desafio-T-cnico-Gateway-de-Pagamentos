package br.com.setis.desafiojava.service.impl;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.service.IdentityProvider;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService implements IdentityProvider {

  private final Keycloak keycloak;

  @Value("${keycloak.realm}")
  private String realm;

  @Override
  public String cadastrarUsuario(CriarUsuarioLojistaRequest usuarioLojista, UUID idLojista) {
    UsersResource usersResource = keycloak.realm(realm).users();
    final var user = newUserRepresentation(usuarioLojista, idLojista.toString());
    String userId = null;

    try (Response response = usersResource.create(user)) {
      if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
        log.error("Erro Keycloak: {} - Status: {}", response.getStatusInfo(), response.getStatus());
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Erro ao criar usuário no provedor de identidade.");
      }

      String path = response.getLocation().getPath();
      userId = path.substring(path.lastIndexOf("/") + 1);

      log.info("Novo usuário do lojista {} cadastrado no IDP: {}", idLojista, userId);

      atribuirRole(usersResource, userId, usuarioLojista.cargo());

      return userId;

    } catch (Exception e) {
      if (userId != null) {
        executarRollback(usersResource, userId);
      }

      if ("Erro ao criar usuário no provedor de identidade.".equals(e.getMessage())) {
        throw e;
      }

      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao criar usuário no IDP", e);
    }
  }

  private void executarRollback(UsersResource usersResource, String userId) {
    log.warn("Falha no processo, iniciando rollback no IDP para usuário {}", userId);
    try (Response response = usersResource.delete(userId)) {
      if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
        log.error(
            "CRÍTICO: Erro ao realizar rollback do usuário {}. O usuário ficou órfão.", userId);
      } else {
        log.info("Rollback de usuário realizado com sucesso");
      }
    } catch (Exception ex) {
      log.error("CRÍTICO: Exceção ao tentar rollback do usuário {}", userId, ex);
    }
  }

  private @NonNull UserRepresentation newUserRepresentation(
      CriarUsuarioLojistaRequest usuarioLojista, String idLojista) {
    UserRepresentation user = new UserRepresentation();
    user.setUsername(usuarioLojista.email());
    user.setEmail(usuarioLojista.email());
    user.setFirstName(usuarioLojista.nome());
    user.setEnabled(true);
    user.setEmailVerified(true);

    user.setAttributes(Map.of("merchant_id", List.of(idLojista)));

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(usuarioLojista.senha());
    credential.setTemporary(false);
    user.setCredentials(List.of(credential));
    return user;
  }

  private void atribuirRole(UsersResource usersResource, String userId, Cargo cargo) {
    try {
      RoleRepresentation role = keycloak.realm(realm).roles().get(cargo.name()).toRepresentation();
      usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(role));

    } catch (Exception e) {
      log.error("Erro ao atribuir role {} ao usuário {}", cargo.name(), userId, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao atribuir permissões");
    }
  }
}
