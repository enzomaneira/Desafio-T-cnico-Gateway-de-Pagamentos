package br.com.setis.desafiojava.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.domain.entity.Provedor;
import br.com.setis.desafiojava.dto.lojista.CriarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.EditarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.ProvedoresRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaInicial;
import br.com.setis.desafiojava.repository.LojistaRepository;
import br.com.setis.desafiojava.service.impl.LojistaServiceImpl;
import java.util.*;
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
public class LojistaServiceTest {

  @Mock private LojistaRepository lojistaRepository;
  @Mock private UsuarioLojistaService usuarioLojistaService;

  @InjectMocks private LojistaServiceImpl lojistaService;

  @Captor private ArgumentCaptor<Lojista> lojistaCaptor;

  @Test
  @DisplayName("Deve cadastrar um lojista com sucesso quando todos dados são válidos")
  void deveCadastrarLojistaComSucesso() {
    var usuarioInicial = new CriarUsuarioLojistaInicial("Stefano", "tef@puc.com", "blinder123");
    var novoLojistaRequest =
        new CriarLojistaRequest("15.008.043/0001-90", "Blinder Inc.", usuarioInicial);

    when(usuarioLojistaService.existePorEmail(usuarioInicial.email()))
        .thenReturn(false); // simule que esse email não existe no sistema
    when(lojistaRepository.existsByCnpj(novoLojistaRequest.cnpj()))
        .thenReturn(false); // simule que esse cnpj não existe no sistema
    when(lojistaRepository.save(any(Lojista.class))) // aceite qualquer objeto Lojista
        .thenAnswer(
            invocation ->
                invocation.getArgument(
                    0)); // devolve de volta o mesmo objeto que foi passado como argumento

    Lojista resultado = lojistaService.cadastrarLojista(novoLojistaRequest); // execute o método

    // verifica se o objeto contém os dados esperados
    Assertions.assertNotNull(resultado);
    Assertions.assertEquals("15.008.043/0001-90", resultado.getCnpj());
    Assertions.assertEquals("Blinder Inc.", resultado.getNomeFantasia());

    verify(lojistaRepository)
        .save(
            lojistaCaptor
                .capture()); // checando se o método save foi realmente chamado durante a execução
    Lojista lojistaSalvo =
        lojistaCaptor.getValue(); // captura o objeto exato que foi passado para save()
    Assertions.assertEquals("15.008.043/0001-90", lojistaSalvo.getCnpj());
  }

  @Test
  @DisplayName("Deve falhar o cadastro de um lojista quando o primeiro usuário estiver cadastrado")
  void deveFalharQuandoEmailExiste() {
    var usuarioInicial = new CriarUsuarioLojistaInicial("Stefano", "stefano@teste.com", "senha123");
    var novoLojista =
        new CriarLojistaRequest("15.008.043/0001-90", "Empresas Inc.", usuarioInicial);

    when(usuarioLojistaService.existePorEmail(usuarioInicial.email())).thenReturn(true);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              lojistaService.cadastrarLojista(novoLojista);
            });

    Assertions.assertEquals(
        "e-mail passado para primeiro usuário já cadastrado", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve falhar o cadastro de um lojista quando o CNPJ utilizado já estiver cadastrado")
  void deveFalharQuandoCnpjExiste() {
    var usuarioInicial = new CriarUsuarioLojistaInicial("Stefano", "stefano@teste.com", "senha123");
    var novoLojista =
        new CriarLojistaRequest("15.008.043/0001-90", "Empresas Inc.", usuarioInicial);

    when(usuarioLojistaService.existePorEmail(usuarioInicial.email())).thenReturn(false);
    when(lojistaRepository.existsByCnpj(novoLojista.cnpj())).thenReturn(true);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              lojistaService.cadastrarLojista(novoLojista);
            });

    Assertions.assertEquals("CNPJ utilizado na requisição já cadastrado", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve retornar paginação contendo lojistas corretamente")
  void deveListarLojistasPaginados() {
    Pageable pageable = Pageable.unpaged();
    Lojista lojista = Lojista.builder().nomeFantasia("Loja 1").build();
    Page<Lojista> paginaRetornada = new PageImpl<>(List.of(lojista));

    when(lojistaRepository.findAll(pageable)).thenReturn(paginaRetornada);

    Page<Lojista> resultado = lojistaService.listarLojistas(pageable);

    Assertions.assertNotNull(resultado);
    Assertions.assertEquals(1, resultado.getTotalElements());
    Assertions.assertEquals("Loja 1", resultado.getContent().getFirst().getNomeFantasia());
    verify(lojistaRepository).findAll(pageable);
  }

  @Test
  @DisplayName("Deve encontrar lojista por ID válido")
  void deveEncontrarLojistaPorId() {
    UUID id = UUID.randomUUID();
    Lojista lojistaEsperado = Lojista.builder().id(id).nomeFantasia("Loja Encontrada").build();

    when(lojistaRepository.findById(id)).thenReturn(Optional.of(lojistaEsperado));

    Optional<Lojista> resultado = lojistaService.listarLojistaPorId(id.toString());

    Assertions.assertTrue(resultado.isPresent());
    Assertions.assertEquals(id, resultado.get().getId());
    verify(lojistaRepository).findById(id);
  }

  @Test
  @DisplayName("Deve retornar vazio quando ID não existe")
  void deveRetornarVazioQuandoIdNaoExiste() {
    UUID id = UUID.randomUUID();
    when(lojistaRepository.findById(id)).thenReturn(Optional.empty());

    Optional<Lojista> resultado = lojistaService.listarLojistaPorId(id.toString());

    Assertions.assertTrue(resultado.isEmpty());
  }

  @Test
  @DisplayName("Deve lançar exceção para ID com formato inválido")
  void deveFalharParaIdInvalido() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          lojistaService.listarLojistaPorId("formato-invalido-nao-uuid");
        });

    verify(lojistaRepository, never()).findById(any());
  }

  @Test
  @DisplayName(
      "Deve atualizar corretamente os dados do lojista quando as informações passadas são válidas")
  void deveAtualizarLojista() {
    UUID id = UUID.randomUUID();
    Lojista lojistaOriginal =
        Lojista.builder()
            .id(id)
            .nomeFantasia("Nome Antigo")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .build();

    var requestAtualizacao = new EditarLojistaRequest("47.863.842/0001-30", "Nome Novo", false);

    when(lojistaRepository.findById(id)).thenReturn(Optional.of(lojistaOriginal));
    when(lojistaRepository.save(any(Lojista.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    lojistaService.atualizarPorId(id.toString(), requestAtualizacao);

    verify(lojistaRepository).save(lojistaCaptor.capture());
    Lojista lojistaAtualizado = lojistaCaptor.getValue();

    Assertions.assertEquals(requestAtualizacao.nomeFantasia(), lojistaAtualizado.getNomeFantasia());
    Assertions.assertEquals(requestAtualizacao.cnpj(), lojistaAtualizado.getCnpj());
    Assertions.assertFalse(lojistaAtualizado.isAtivo());
  }

  @Test
  @DisplayName("Deve lançar exceção para ID não encontrado")
  void deveFalharParaIdNaoEncontrado() {
    UUID id = UUID.randomUUID();
    var requestAtualizacao = new EditarLojistaRequest("47.863.842/0001-30", "Nome Novo", false);

    when(lojistaRepository.findById(id)).thenReturn(Optional.empty());
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              lojistaService.atualizarPorId(id.toString(), requestAtualizacao);
            });

    Assertions.assertEquals("Lojista não encontrado, verificar ID passado", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve cadastrar novos provedores para o Lojista")
  void deveCadastrarNovosProvedores() {
    UUID id = UUID.randomUUID();

    Lojista lojista =
        Lojista.builder()
            .id(id)
            .nomeFantasia("Empresas INC")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .provedores(new HashSet<>())
            .build();

    ProvedoresRequest request = new ProvedoresRequest(Set.of(Provedor.GETNET, Provedor.REDE));

    when(lojistaRepository.findById(id)).thenReturn(Optional.of(lojista));
    when(lojistaRepository.save(any(Lojista.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    lojistaService.cadastrarNovosProvedores(id.toString(), request);

    verify(lojistaRepository).save(lojistaCaptor.capture());
    Lojista lojistaSalvo = lojistaCaptor.getValue();

    Assertions.assertEquals(lojista.getId(), lojistaSalvo.getId());
    Assertions.assertTrue(lojistaSalvo.getProvedores().containsAll(request.provedores()));
  }

  @Test
  @DisplayName("Retornar exceção caso o Id não seja encontrado ao adicionar provedor")
  void deveRetornarExcecaoCasoNaoAcheIdAoAdicionarProvedor() {
    UUID id = UUID.randomUUID();
    ProvedoresRequest request = new ProvedoresRequest(Set.of(Provedor.GETNET, Provedor.REDE));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              lojistaService.cadastrarNovosProvedores(id.toString(), request);
            });

    Assertions.assertEquals("Lojista não encontrado, verificar ID passado", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "Deve iniciar um hashset vazio e adicionar todos provedores caso o set de provedores seja"
          + " null")
  void deveTratarCorretamenteSetNullDeProvedores() {
    UUID id = UUID.randomUUID();

    Lojista lojista =
        Lojista.builder()
            .id(id)
            .nomeFantasia("Empresas INC")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .provedores(null)
            .build();

    ProvedoresRequest request = new ProvedoresRequest(Set.of(Provedor.GETNET, Provedor.REDE));

    when(lojistaRepository.findById(id)).thenReturn(Optional.of(lojista));
    when(lojistaRepository.save(any(Lojista.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    lojistaService.cadastrarNovosProvedores(id.toString(), request);

    verify(lojistaRepository).save(lojistaCaptor.capture());
    Lojista lojistaSalvo = lojistaCaptor.getValue();

    Assertions.assertEquals(lojista.getId(), lojistaSalvo.getId());
    Assertions.assertTrue(lojistaSalvo.getProvedores().containsAll(request.provedores()));
  }

  @Test
  @DisplayName("Deve remover corretamente a lista de provedores passados do lojista")
  void deveRemoverProvedoresPassados() {
    UUID id = UUID.randomUUID();
    ProvedoresRequest request =
        new ProvedoresRequest(
            new HashSet<>(
                Set.of(Provedor.GETNET, Provedor.REDE, Provedor.SICOOB, Provedor.SICREDI)));
    ProvedoresRequest provedoresARemover =
        new ProvedoresRequest(Set.of(Provedor.REDE, Provedor.SICREDI));

    Lojista lojista =
        Lojista.builder()
            .id(id)
            .nomeFantasia("Empresas INC")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .provedores(request.provedores())
            .build();

    when(lojistaRepository.findById(id)).thenReturn(Optional.of(lojista));
    when(lojistaRepository.save(any(Lojista.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    lojistaService.removerProvedores(id.toString(), provedoresARemover);

    verify(lojistaRepository).save(lojistaCaptor.capture());
    Lojista lojistaSalvo = lojistaCaptor.getValue();

    Assertions.assertEquals(lojista.getId(), lojistaSalvo.getId());
    Assertions.assertTrue(
        lojistaSalvo.getProvedores().containsAll(Set.of(Provedor.GETNET, Provedor.SICOOB)));
    Assertions.assertFalse(lojistaSalvo.getProvedores().contains(Provedor.REDE));
  }

  @Test
  @DisplayName(
      "Deve lançar exceção caso tente remover provedores e o lojista tenha lista vazia de"
          + " provedores")
  void deveLancarExcecaoCasoTenteRemoverProvedores() {
    UUID id = UUID.randomUUID();
    ProvedoresRequest provedoresARemover =
        new ProvedoresRequest(Set.of(Provedor.REDE, Provedor.SICREDI));

    Lojista lojista =
        Lojista.builder()
            .id(id)
            .nomeFantasia("Empresas INC")
            .cnpj("03.947.368/0001-50")
            .ativo(true)
            .provedores(new HashSet<>())
            .build();

    when(lojistaRepository.findById(id)).thenReturn(Optional.of(lojista));
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              lojistaService.removerProvedores(id.toString(), provedoresARemover);
            });

    Assertions.assertEquals(
        "Lojista não possuí nenhum provedor casdastrado", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }

  @Test
  @DisplayName("Retornar exceção caso o Id não seja encontrado ao remover provedor")
  void deveRetornarExcecaoCasoNaoAcheIdAoRemoverProvedor() {
    UUID id = UUID.randomUUID();
    ProvedoresRequest request = new ProvedoresRequest(Set.of(Provedor.GETNET, Provedor.REDE));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              lojistaService.removerProvedores(id.toString(), request);
            });

    Assertions.assertEquals("Lojista não encontrado, verificar ID passado", exception.getMessage());
    verify(lojistaRepository, never()).save(any());
  }
}
