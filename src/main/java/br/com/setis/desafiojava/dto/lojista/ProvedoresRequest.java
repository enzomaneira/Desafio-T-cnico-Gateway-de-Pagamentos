package br.com.setis.desafiojava.dto.lojista;

import br.com.setis.desafiojava.domain.entity.Provedor;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record ProvedoresRequest(
    @NotEmpty(message = "A lista de provedores obrigatória") Set<Provedor> provedores) {}
