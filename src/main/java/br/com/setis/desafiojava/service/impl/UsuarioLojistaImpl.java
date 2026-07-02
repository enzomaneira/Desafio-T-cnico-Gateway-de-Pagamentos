package br.com.setis.desafiojava.service.impl;

import static br.com.setis.desafiojava.domain.entity.Cargo.CARGOS_PERMITIDOS_GERENTE;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.domain.entity.UsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.EditarUsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import br.com.setis.desafiojava.mapper.UsuarioLojistaMapper;
import br.com.setis.desafiojava.repository.LojistaRepository;
import br.com.setis.desafiojava.repository.UsuarioLojistaRepository;
import br.com.setis.desafiojava.service.IdentityProvider;
import br.com.setis.desafiojava.service.UsuarioLojistaService;
import jakarta.transaction.Transactional;
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
public class UsuarioLojistaImpl implements UsuarioLojistaService {
  private final UsuarioLojistaRepository usuarioLojistaRepository;
  private final LojistaRepository lojistaRepository;
  private final IdentityProvider identityProvider;
  private final UsuarioLojistaMapper usuarioMapper;

  @Override
  @Transactional
  public UsuarioLojistaDto cadastrarUsuarioLojista(
      CriarUsuarioLojistaRequest request, UUID idLojista, Cargo cargoSolicitante) {
    validarPermissoesDeCargo(cargoSolicitante, request.cargo());

    if (usuarioLojistaRepository.existsByEmail(request.email())) {
      log.error("e-mail passado para  usuário já cadastrado");
      throw new IllegalArgumentException("e-mail passado para usuário já cadastrado");
    }

    String idpId = identityProvider.cadastrarUsuario(request, idLojista);

    Lojista proxyLojista = lojistaRepository.getReferenceById(idLojista);

    UsuarioLojista usuarioLojista =
        UsuarioLojista.builder()
            .nome(request.nome())
            .email(request.email())
            .idpId(idpId)
            .cargo(request.cargo())
            .lojista(proxyLojista)
            .build();

    log.info(
        "Novo usuário do lojista {} cadastrado no sistema", usuarioLojista.getLojista().getCnpj());

    usuarioLojista = usuarioLojistaRepository.save(usuarioLojista);
    return usuarioMapper.toDto(usuarioLojista);
  }

  @Override
  public Page<UsuarioLojistaDto> listarUsuariosPorLojista(Pageable pageable, String lojistaId) {
    return usuarioLojistaRepository
        .findAllByLojista_Id(pageable, UUID.fromString(lojistaId))
        .map(usuarioMapper::toDto);
  }

  @Override
  public boolean existePorEmail(String email) {
    return usuarioLojistaRepository.existsByEmail(email);
  }

  @Override
  public Optional<UsuarioLojistaDto> listarUsuarioPorLojista(String usuarioId, String lojistaId) {
    return usuarioLojistaRepository
        .findByIdAndLojista_Id(UUID.fromString(usuarioId), UUID.fromString(lojistaId))
        .map(usuarioMapper::toDto);
  }

  @Override
  @Transactional
  public UsuarioLojistaDto atualizarUsuarioPorId(
      String usuarioId, String lojistaId, EditarUsuarioLojista request) {
    UsuarioLojista usuarioLojista =
        usuarioLojistaRepository
            .findByIdAndLojista_Id(UUID.fromString(usuarioId), UUID.fromString(lojistaId))
            .orElseThrow(
                () -> {
                  log.error("Usuario {} não encontrado neste lojista {}", usuarioId, lojistaId);
                  return new IllegalArgumentException("Usuario não encontrado neste lojista");
                });

    usuarioLojista.setNome(request.nome());
    usuarioLojista.setCargo(request.cargo());
    usuarioLojista.setAtivo(request.ativo());

    return usuarioMapper.toDto(usuarioLojistaRepository.save(usuarioLojista));
  }

  private void validarPermissoesDeCargo(Cargo cargoSolicitante, Cargo cargoDesejado) {
    if (cargoSolicitante == Cargo.ADMINISTRADOR) {
      return;
    }

    if (cargoSolicitante == Cargo.GERENTE) {
      if (CARGOS_PERMITIDOS_GERENTE.contains(cargoDesejado)) {
        return;
      }
      throw new IllegalArgumentException(
          "Gerentes só podem criar usuários de nível Gerente ou Analista");
    }

    throw new IllegalArgumentException(
        "O cargo " + cargoSolicitante + " não tem permissão para criar usuários");
  }
}
