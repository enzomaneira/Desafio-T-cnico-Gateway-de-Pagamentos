package br.com.setis.desafiojava.repository;

import br.com.setis.desafiojava.domain.entity.Transacao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransacaoRepository
    extends JpaRepository<Transacao, UUID>, JpaSpecificationExecutor<Transacao> {
  Optional<Transacao> findByIdAndLojista_Id(UUID id, UUID lojistaId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM Transacao t WHERE t.id = :txId AND t.lojista.id = :lojistaId")
  Optional<Transacao> findByIdWithLock(UUID txId, UUID lojistaId);
}
