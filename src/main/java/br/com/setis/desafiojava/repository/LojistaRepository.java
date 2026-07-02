package br.com.setis.desafiojava.repository;

import br.com.setis.desafiojava.domain.entity.Lojista;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LojistaRepository extends JpaRepository<Lojista, UUID> {
  boolean existsByCnpj(String cnpj);
}
