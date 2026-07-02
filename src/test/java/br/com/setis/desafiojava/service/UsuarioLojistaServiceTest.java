package br.com.setis.desafiojava.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.domain.entity.UsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.EditarUsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import br.com.setis.desafiojava.mapper.UsuarioLojistaMapper;
import br.com.setis.desafiojava.repository.LojistaRepository;
import br.com.setis.desafiojava.repository.UsuarioLojistaRepository;
import br.com.setis.desafiojava.service.impl.UsuarioLojistaImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class UsuarioLojistaServiceTest {
  @Mock private UsuarioLojistaRepository usuarioLojistaRepository;
  @Mock private LojistaRepository lojistaRepository;
  @Mock private IdentityProvider identityProvider;
  @Mock private UsuarioLojistaMapper usuarioLojistaMapper;

  @InjectMocks private UsuarioLojistaImpl usuarioLojistaService;

  @Captor private ArgumentCaptor<UsuarioLojista> usuarioCaptor;

  @Test
  @DisplayName("Deve cadastrar um usuário corretamente quando as informações passadas são válidas")
  void deveCadastrarUmUsuarioLojista() {
    UUID idLojista = UUID.randomUUID();
    String idpIdGerado = "idp-id-123";

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Stefano", "teste@teste.com", "123456", Cargo.GERENTE);

    Lojista lojistaProxy = Lojista.builder().id(idLojista).build();

    when(identityProvider.cadastrarUsuario(request, idLojista)).thenReturn(idpIdGerado);
    when(lojistaRepository.getReferenceById(idLojista)).thenReturn(lojistaProxy);
    when(usuarioLojistaRepository.save(any(UsuarioLojista.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(usuarioLojistaMapper.toDto(any())).thenReturn(mock(UsuarioLojistaDto.class));

    usuarioLojistaService.cadastrarUsuarioLojista(request, idLojista, Cargo.ADMINISTRADOR);

    verify(usuarioLojistaRepository).save(usuarioCaptor.capture());
    UsuarioLojista usuarioSalvo = usuarioCaptor.getValue();

    Assertions.assertEquals(request.nome(), usuarioSalvo.getNome());
    Assertions.assertEquals(request.email(), usuarioSalvo.getEmail());
    Assertions.assertEquals(request.cargo(), usuarioSalvo.getCargo());
    Assertions.assertEquals(idpIdGerado, usuarioSalvo.getIdpId());
    Assertions.assertEquals(idLojista, usuarioSalvo.getLojista().getId());
    Assertions.assertTrue(usuarioSalvo.isAtivo());

    verify(identityProvider).cadastrarUsuario(request, idLojista);
  }

  @Test
  @DisplayName("Deve falhar cadastro quando um gerente tenta criar um usuário administrador")
  void deveFalharCadastroQuandoGerenteTentaCriarAdm() {
    UUID idLojista = UUID.randomUUID();

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Stefano", "teste@teste.com", "123456", Cargo.ADMINISTRADOR);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              usuarioLojistaService.cadastrarUsuarioLojista(request, idLojista, Cargo.GERENTE);
            });

    Assertions.assertEquals(
        "Gerentes só podem criar usuários de nível Gerente ou Analista", exception.getMessage());

    verify(usuarioLojistaRepository, never()).save(any());
    verifyNoInteractions(identityProvider);
  }

  @Test
  @DisplayName("Deve falhar cadastro quando um analista tenta criar um usuário")
  void deveFalharQuandoAnalistaTentaCriarUsuario() {
    UUID idLojista = UUID.randomUUID();

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Stefano", "teste@teste.com", "123456", Cargo.ADMINISTRADOR);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              usuarioLojistaService.cadastrarUsuarioLojista(request, idLojista, Cargo.ANALISTA);
            });

    Assertions.assertEquals(
        "O cargo ANALISTA não tem permissão para criar usuários", exception.getMessage());

    verify(usuarioLojistaRepository, never()).save(any());
    verifyNoInteractions(identityProvider);
  }

  @Test
  @DisplayName("Deve retornar paginação contendo usuários de lojistas corretamente")
  void deveListarUsuariosLojistaPaginados() {
    UUID idUsuario = UUID.randomUUID();
    UUID idLojista = UUID.randomUUID();

    Lojista lojistaProxy = Lojista.builder().id(idLojista).build();
    UsuarioLojista usuarioLojista =
        UsuarioLojista.builder()
            .id(idUsuario)
            .nome("Stefano")
            .lojista(lojistaProxy)
            .idpId("idp-id-123")
            .email("teste@teste.com")
            .cargo(Cargo.GERENTE)
            .ativo(true)
            .dataCriacao(LocalDateTime.now())
            .dataAtualizacao(LocalDateTime.now())
            .build();

    UsuarioLojistaDto usuarioDtoEsperado =
        new UsuarioLojistaDto(
            usuarioLojista.getId(),
            usuarioLojista.getNome(),
            usuarioLojista.getEmail(),
            usuarioLojista.getCargo(),
            usuarioLojista.isAtivo(),
            idLojista,
            null,
            usuarioLojista.getDataCriacao(),
            usuarioLojista.getDataAtualizacao());

    Pageable pageable = Pageable.unpaged();

    Page<UsuarioLojista> paginaDeEntidades = new PageImpl<>(List.of(usuarioLojista));

    when(usuarioLojistaRepository.findAllByLojista_Id(pageable, idLojista))
        .thenReturn(paginaDeEntidades);
    when(usuarioLojistaMapper.toDto(usuarioLojista)).thenReturn(usuarioDtoEsperado);

    Page<UsuarioLojistaDto> resultado =
        usuarioLojistaService.listarUsuariosPorLojista(pageable, idLojista.toString());

    Assertions.assertNotNull(resultado);
    Assertions.assertEquals(1, resultado.getTotalElements());
    Assertions.assertEquals("Stefano", resultado.getContent().getFirst().nome());

    verify(usuarioLojistaRepository).findAllByLojista_Id(pageable, idLojista);
  }

  @Test
  @DisplayName("Deve encontrar usuário por ID válido de Lojista e Usuário")
  void deveEncontrarUsuarioLojistaPorId() {
    UUID idUsuario = UUID.randomUUID();
    UUID idLojista = UUID.randomUUID();

    Lojista lojistaProxy = Lojista.builder().id(idLojista).build();
    UsuarioLojista usuarioLojista =
        UsuarioLojista.builder()
            .id(idUsuario)
            .nome("Stefano")
            .lojista(lojistaProxy)
            .idpId("idp-id-123")
            .email("teste@teste.com")
            .cargo(Cargo.GERENTE)
            .ativo(true)
            .dataCriacao(LocalDateTime.now())
            .dataAtualizacao(LocalDateTime.now())
            .build();

    UsuarioLojistaDto usuarioDtoEsperado =
        new UsuarioLojistaDto(
            usuarioLojista.getId(),
            usuarioLojista.getNome(),
            usuarioLojista.getEmail(),
            usuarioLojista.getCargo(),
            usuarioLojista.isAtivo(),
            idLojista,
            null,
            usuarioLojista.getDataCriacao(),
            usuarioLojista.getDataAtualizacao());

    when(usuarioLojistaRepository.findByIdAndLojista_Id(idUsuario, idLojista))
        .thenReturn(Optional.of(usuarioLojista));
    when(usuarioLojistaMapper.toDto(usuarioLojista)).thenReturn(usuarioDtoEsperado);

    Optional<UsuarioLojistaDto> resultado =
        usuarioLojistaService.listarUsuarioPorLojista(idUsuario.toString(), idLojista.toString());

    Assertions.assertTrue(resultado.isPresent());
    Assertions.assertEquals(usuarioDtoEsperado, resultado.get());
    verify(usuarioLojistaRepository).findByIdAndLojista_Id(idUsuario, idLojista);
  }

  @Test
  @DisplayName("Deve retornar vazio quando ID não existe")
  void deveRetornarVazioQuandoIdNaoExiste() {
    UUID idUsuario = UUID.randomUUID();
    UUID idLojista = UUID.randomUUID();
    when(usuarioLojistaRepository.findByIdAndLojista_Id(idUsuario, idLojista))
        .thenReturn(Optional.empty());

    Optional<UsuarioLojistaDto> resultado =
        usuarioLojistaService.listarUsuarioPorLojista(idUsuario.toString(), idLojista.toString());

    Assertions.assertTrue(resultado.isEmpty());
  }

  @Test
  @DisplayName("Deve lançar exceção para ID com formato inválido")
  void deveFalharParaIdInvalido() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          usuarioLojistaService.listarUsuarioPorLojista(
              "formato-invalido-nao-uuid", "formato-invalido");
        });

    verify(lojistaRepository, never()).findById(any());
  }

  @Test
  @DisplayName(
      "Deve atualizar corretamente os dados do usuário quando as informações passadas são válidas")
  void deveAtualizarUsuarioLojista() {
    UUID idUsuario = UUID.randomUUID();
    UUID idLojista = UUID.randomUUID();

    UsuarioLojista usuarioOriginal =
        UsuarioLojista.builder()
            .id(idUsuario)
            .nome("Stefano Antigo")
            .cargo(Cargo.GERENTE)
            .ativo(true)
            .build();

    var request = new EditarUsuarioLojista("Stefano Novo", Cargo.ANALISTA, false);

    when(usuarioLojistaRepository.findByIdAndLojista_Id(idUsuario, idLojista))
        .thenReturn(Optional.of(usuarioOriginal));
    when(usuarioLojistaRepository.save(any(UsuarioLojista.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(usuarioLojistaMapper.toDto(any())).thenReturn(mock(UsuarioLojistaDto.class));

    usuarioLojistaService.atualizarUsuarioPorId(
        idUsuario.toString(), idLojista.toString(), request);

    verify(usuarioLojistaRepository).save(usuarioCaptor.capture());
    UsuarioLojista usuarioAtualizado = usuarioCaptor.getValue();

    Assertions.assertEquals("Stefano Novo", usuarioAtualizado.getNome());
    Assertions.assertEquals(Cargo.ANALISTA, usuarioAtualizado.getCargo());
    Assertions.assertFalse(usuarioAtualizado.isAtivo());
  }

  @Test
  @DisplayName("Deve lançar exceção para ID não encontrado")
  void deveFlaharParaIdInvalido() {
    UUID idUsuario = UUID.randomUUID();
    UUID idLojista = UUID.randomUUID();

    var request = new EditarUsuarioLojista("Nome Novo", Cargo.GERENTE, true);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              usuarioLojistaService.atualizarUsuarioPorId(
                  idUsuario.toString(), idLojista.toString(), request);
            });

    Assertions.assertEquals("Usuario não encontrado neste lojista", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve retornar verdadeiro se email já existe")
  void deveRetornarVerdadeiroSeEmailJaExiste() {
    String email = "teste@teste.com";

    when(usuarioLojistaRepository.existsByEmail(email)).thenReturn(true);

    boolean existe = usuarioLojistaService.existePorEmail(email);

    Assertions.assertTrue(existe);
    verify(usuarioLojistaRepository).existsByEmail(email);
  }

  @Test
  @DisplayName("Deve permitir que Gerente crie usuário com cargo Analista")
  void devePermiterGerenteCriarAnalista() {
    UUID idLojista = UUID.randomUUID();
    UUID idUsuario = UUID.randomUUID();
    String idpId = "idp-id-123";

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Stefano", "teste@teste.com", "123456", Cargo.ANALISTA);

    Lojista lojistaProxy = Lojista.builder().id(idLojista).build();

    when(identityProvider.cadastrarUsuario(request, idLojista)).thenReturn(idpId);
    when(lojistaRepository.getReferenceById(idLojista)).thenReturn(lojistaProxy);
    when(usuarioLojistaRepository.save(any(UsuarioLojista.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(usuarioLojistaMapper.toDto(any())).thenReturn(mock(UsuarioLojistaDto.class));

    usuarioLojistaService.cadastrarUsuarioLojista(request, idLojista, Cargo.GERENTE);

    verify(usuarioLojistaRepository).save(usuarioCaptor.capture());
    UsuarioLojista usuarioSalvo = usuarioCaptor.getValue();

    Assertions.assertEquals(request.nome(), usuarioSalvo.getNome());
    Assertions.assertEquals(Cargo.ANALISTA, usuarioSalvo.getCargo());

    verify(identityProvider).cadastrarUsuario(request, idLojista);
  }

  @Test
  @DisplayName("Deve falhar ao cadastrar usuário com email já existente")
  void deveFalharCadastroEmailDuplicado() {
    UUID idLojista = UUID.randomUUID();
    String emailExistente = "duplicado@teste.com";

    CriarUsuarioLojistaRequest request =
        new CriarUsuarioLojistaRequest("Stefano", emailExistente, "123456", Cargo.GERENTE);

    when(usuarioLojistaRepository.existsByEmail(emailExistente)).thenReturn(true);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              usuarioLojistaService.cadastrarUsuarioLojista(
                  request, idLojista, Cargo.ADMINISTRADOR);
            });

    Assertions.assertEquals("e-mail passado para usuário já cadastrado", exception.getMessage());

    verifyNoInteractions(identityProvider);
    verify(usuarioLojistaRepository, never()).save(any());
  }
}
