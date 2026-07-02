package br.com.setis.desafiojava.dto.usuarioLojista;

import br.com.setis.desafiojava.domain.entity.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditarUsuarioLojista(
    @NotBlank(message = "Nome obrigatório") String nome,
    @NotNull(message = "Cargo obrigatório") Cargo cargo,
    @NotNull(message = "Status obrigatório") Boolean ativo) {}
