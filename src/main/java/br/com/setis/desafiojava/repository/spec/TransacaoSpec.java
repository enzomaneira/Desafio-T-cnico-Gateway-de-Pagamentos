package br.com.setis.desafiojava.repository.spec;

import br.com.setis.desafiojava.domain.entity.MetodoPagamento;
import br.com.setis.desafiojava.domain.entity.StatusTransacao;
import br.com.setis.desafiojava.domain.entity.Transacao;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class TransacaoSpec {

  public static Specification<Transacao> filtrarPor(
      String lojistaId,
      LocalDate dataInicio,
      LocalDate dataFim,
      StatusTransacao status,
      MetodoPagamento metodo) {
    return (root, query, cb) -> {
      var p = cb.equal(root.get("lojista").get("id"), UUID.fromString(lojistaId));

      if (dataInicio != null) {
        p = cb.and(p, cb.greaterThanOrEqualTo(root.get("dataCriacao"), dataInicio.atStartOfDay()));
      }

      if (dataFim != null) {
        p = cb.and(p, cb.lessThanOrEqualTo(root.get("dataCriacao"), dataFim.atTime(LocalTime.MAX)));
      }

      if (status != null) {
        p = cb.and(p, cb.equal(root.get("status"), status));
      }

      if (metodo != null) {
        p = cb.and(p, cb.equal(root.get("metodoPagamento"), metodo));
      }

      return p;
    };
  }
}
