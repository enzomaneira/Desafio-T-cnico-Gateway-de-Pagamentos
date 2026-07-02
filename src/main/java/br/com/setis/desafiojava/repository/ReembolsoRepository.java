package br.com.setis.desafiojava.repository;

import br.com.setis.desafiojava.domain.entity.Reembolso;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReembolsoRepository extends JpaRepository<Reembolso, UUID> {

  @Query(
      value =
          """
        SELECT r FROM Reembolso r
        JOIN FETCH r.transacao t
        JOIN FETCH t.lojista l
        WHERE t.id = :txId AND l.id = :lojistaId
        """,
      countQuery =
          """
        SELECT count(r) FROM Reembolso r
        WHERE r.transacao.id = :txId AND r.transacao.lojista.id = :lojistaId
        """)
  Page<Reembolso> findAllByTxAndLojista_Id(Pageable pageable, UUID txId, UUID lojistaId);
}
