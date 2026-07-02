package br.com.setis.desafiojava.repository;

import br.com.setis.desafiojava.domain.entity.UsuarioLojista;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioLojistaRepository extends JpaRepository<UsuarioLojista, UUID> {
  boolean existsByEmail(String email);

  Optional<UsuarioLojista> findByEmail(String email);

  Page<UsuarioLojista> findAllByLojista_Id(Pageable pageable, UUID lojistaId);

  Optional<UsuarioLojista> findByIdAndLojista_Id(UUID id, UUID lojistaId);
}
