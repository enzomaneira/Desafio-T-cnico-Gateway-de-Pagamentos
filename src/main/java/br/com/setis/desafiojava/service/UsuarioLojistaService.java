package br.com.setis.desafiojava.service;

import br.com.setis.desafiojava.domain.entity.Cargo;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.EditarUsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioLojistaService {
  UsuarioLojistaDto cadastrarUsuarioLojista(
      CriarUsuarioLojistaRequest usuarioLojista, UUID idLojista, Cargo cargoSolicitante);

  Page<UsuarioLojistaDto> listarUsuariosPorLojista(Pageable pageable, String lojistaId);

  boolean existePorEmail(String email);

  Optional<UsuarioLojistaDto> listarUsuarioPorLojista(String lojistaId, String usuarioId);

  UsuarioLojistaDto atualizarUsuarioPorId(
      String usuarioId, String lojistaId, @Valid EditarUsuarioLojista request);
}
