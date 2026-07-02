package br.com.setis.desafiojava.dto.pagina;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
    List<T> conteudo,
    int paginaAtual,
    int itensPorPagina,
    long totalRegistros,
    int totalPaginas,
    boolean ultimaPagina) {
  public PaginaResponse(Page<T> page) {
    this(
        page.getContent(),
        page.getNumber() + 1,
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }
}
