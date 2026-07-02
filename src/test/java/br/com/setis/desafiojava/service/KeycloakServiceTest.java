package br.com.setis.desafiojava.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.service.impl.KeycloakService;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

  @Mock private Keycloak keycloak;

  @Mock private RealmResource realmResource;
  @Mock private UsersResource usersResource;
  @Mock private UserResource userResource;
  @Mock private RolesResource rolesResource;
  @Mock private RoleResource roleResource;
  @Mock private RoleMappingResource roleMappingResource;
  @Mock private RoleScopeResource roleScopeResource;

  @Mock private Response response;

  @InjectMocks private KeycloakService keycloakService;

  @Test
  @DisplayName("Deve criar usuário e atribuir role com sucesso")
  void deveCriarUsuarioSucesso() {
    configurarVariaveisAmbiente();
    UUID idLojista = UUID.randomUUID();
    var request =
        new CriarUsuarioLojistaRequest("Stefano", "stefano@teste.com", "123", Cargo.GERENTE);

    when(keycloak.realm(anyString())).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);

    when(response.getStatus()).thenReturn(201);
    when(response.getLocation())
        .thenReturn(URI.create("http://localhost/auth/admin/realms/realm/users/novo-id-uuid-123"));
    when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

    when(realmResource.roles()).thenReturn(rolesResource);
    when(rolesResource.get("GERENTE")).thenReturn(roleResource);
    when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());

    when(usersResource.get("novo-id-uuid-123")).thenReturn(userResource);
    when(userResource.roles()).thenReturn(roleMappingResource);
    when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

    String userId = keycloakService.cadastrarUsuario(request, idLojista);

    Assertions.assertEquals("novo-id-uuid-123", userId);

    verify(roleScopeResource).add(anyList());
    verify(response).close();
  }

  @Test
  @DisplayName("Deve lançar exceção quando Keycloak recusa criação (Ex: 409 Conflict)")
  void deveFalharSeNaoCriarUsuario() {
    configurarVariaveisAmbiente();
    var request =
        new CriarUsuarioLojistaRequest("Stefano", "duplicado@teste.com", "123", Cargo.GERENTE);

    when(keycloak.realm(anyString())).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);

    when(response.getStatus()).thenReturn(409); // Conflict
    when(response.getStatusInfo()).thenReturn(Response.Status.CONFLICT);
    when(usersResource.create(any())).thenReturn(response);

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> keycloakService.cadastrarUsuario(request, UUID.randomUUID()));

    Assertions.assertEquals("502 BAD_GATEWAY \"Falha ao criar usuário no IDP\"", ex.getMessage());

    verify(realmResource, never()).roles();
    verify(usersResource, never()).delete(anyString());
  }

  @Test
  @DisplayName("Deve deletar usuário se falhar ao atribuir a Role")
  void deveFazerRollbackSeFalharRole() {
    configurarVariaveisAmbiente();
    var request =
        new CriarUsuarioLojistaRequest("Stefano", "sem-role@teste.com", "123", Cargo.GERENTE);
    UUID idLojista = UUID.randomUUID();

    when(keycloak.realm(anyString())).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);

    when(response.getStatus()).thenReturn(201);
    when(response.getLocation()).thenReturn(URI.create("http://localhost/users/id-para-apagar"));
    when(usersResource.create(any())).thenReturn(response);

    when(realmResource.roles()).thenThrow(new RuntimeException("Erro ao buscar role"));

    Response responseDelete = mock(Response.class);
    when(responseDelete.getStatus()).thenReturn(204);
    when(usersResource.delete("id-para-apagar")).thenReturn(responseDelete);

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> keycloakService.cadastrarUsuario(request, idLojista));

    assertEquals("502 BAD_GATEWAY \"Falha ao criar usuário no IDP\"", ex.getMessage());

    verify(usersResource).delete("id-para-apagar");
  }

  private void configurarVariaveisAmbiente() {
    ReflectionTestUtils.setField(keycloakService, "realm", "teste-realm");
  }
}
