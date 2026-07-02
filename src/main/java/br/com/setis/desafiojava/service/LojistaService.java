package br.com.setis.desafiojava.service;

import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.dto.lojista.CriarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.EditarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.ProvedoresRequest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LojistaService {
  Lojista cadastrarLojista(CriarLojistaRequest request);

  Page<Lojista> listarLojistas(Pageable pageable);

  Optional<Lojista> listarLojistaPorId(String id);

  Lojista atualizarPorId(String id, EditarLojistaRequest lojistaRequest);

  Lojista cadastrarNovosProvedores(String id, ProvedoresRequest request);

  Lojista removerProvedores(String id, ProvedoresRequest request);
}
