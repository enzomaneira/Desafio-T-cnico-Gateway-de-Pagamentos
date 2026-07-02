package br.com.setis.desafiojava.service.impl;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.dto.lojista.CriarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.EditarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.ProvedoresRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.repository.LojistaRepository;
import br.com.setis.desafiojava.service.LojistaService;
import br.com.setis.desafiojava.service.UsuarioLojistaService;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LojistaServiceImpl implements LojistaService {

  private final LojistaRepository lojistaRepository;
  private final UsuarioLojistaService usuarioLojistaService;

  @Override
  @Transactional
  public Lojista cadastrarLojista(CriarLojistaRequest request) {
    var usuarioLojistaInicial = request.usuarioInicial();
    boolean usuarioJaCadastrado =
        usuarioLojistaService.existePorEmail(usuarioLojistaInicial.email());
    if (usuarioJaCadastrado) {
      log.error("e-mail passado para primeiro usuário já cadastrado");
      throw new IllegalArgumentException("e-mail passado para primeiro usuário já cadastrado");
    }

    boolean cnpjJaCadastrado = lojistaRepository.existsByCnpj(request.cnpj());
    if (cnpjJaCadastrado) {
      log.error("CNPJ utilizado na requisição já cadastrado");
      throw new IllegalArgumentException("CNPJ utilizado na requisição já cadastrado");
    }

    Lojista lojista =
        Lojista.builder().cnpj(request.cnpj()).nomeFantasia(request.nomeFantasia()).build();

    Lojista novoLojista = lojistaRepository.save(lojista);
    log.info(
        "Novo lojista de CNPJ {} cadastrado com id {}", novoLojista.getCnpj(), novoLojista.getId());

    var usuarioLojistaRequest =
        new CriarUsuarioLojistaRequest(
            usuarioLojistaInicial.nome(),
            usuarioLojistaInicial.email(),
            usuarioLojistaInicial.senha(),
            Cargo.GERENTE);

    // Aqui já se assume que o cargo é adminstrativo já que o controller faz verificação do cargo
    // administrador
    usuarioLojistaService.cadastrarUsuarioLojista(
        usuarioLojistaRequest, novoLojista.getId(), Cargo.ADMINISTRADOR);

    return novoLojista;
  }

  @Override
  public Page<Lojista> listarLojistas(Pageable pageable) {
    return lojistaRepository.findAll(pageable);
  }

  @Override
  public Optional<Lojista> listarLojistaPorId(String id) {
    return lojistaRepository.findById(UUID.fromString(id));
  }

  @Override
  @Transactional
  public Lojista atualizarPorId(String id, EditarLojistaRequest lojistaRequest) {
    Lojista lojistaEncontrado =
        lojistaRepository
            .findById(UUID.fromString(id))
            .orElseThrow(
                () -> {
                  log.error("Lojista não encontrado, verificar ID passado: [{}]", id);
                  return new IllegalArgumentException(
                      "Lojista não encontrado, verificar ID passado");
                });

    lojistaEncontrado.setNomeFantasia(lojistaRequest.nomeFantasia());
    lojistaEncontrado.setCnpj(lojistaRequest.cnpj());
    lojistaEncontrado.setAtivo(lojistaRequest.ativo());

    return lojistaRepository.save(lojistaEncontrado);
  }

  @Override
  @Transactional
  public Lojista cadastrarNovosProvedores(String id, ProvedoresRequest request) {
    Lojista lojistaEncontrado =
        lojistaRepository
            .findById(UUID.fromString(id))
            .orElseThrow(
                () -> {
                  log.error("Lojista não encontrado, verificar ID passado: [{}]", id);
                  return new IllegalArgumentException(
                      "Lojista não encontrado, verificar ID passado");
                });

    if (lojistaEncontrado.getProvedores() == null) {
      lojistaEncontrado.setProvedores(new HashSet<>());
    }

    lojistaEncontrado.getProvedores().addAll(request.provedores());
    return lojistaRepository.save(lojistaEncontrado);
  }

  @Override
  @Transactional
  public Lojista removerProvedores(String id, ProvedoresRequest request) {
    Lojista lojistaEncontrado =
        lojistaRepository
            .findById(UUID.fromString(id))
            .orElseThrow(
                () -> {
                  log.error("Lojista não encontrado, verificar ID passado: [{}]", id);
                  return new IllegalArgumentException(
                      "Lojista não encontrado, verificar ID passado");
                });

    if (lojistaEncontrado.getProvedores().isEmpty()) {
      log.error("Lojista [{}] não possuí nenhum provedor casdastrado", id);
      throw new IllegalArgumentException("Lojista não possuí nenhum provedor casdastrado");
    }

    lojistaEncontrado.getProvedores().removeAll(request.provedores());
    return lojistaRepository.save(lojistaEncontrado);
  }
}
