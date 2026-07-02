package br.com.setis.desafiojava.dto.usuarioLojista;

import br.com.setis.desafiojava.domain.entity.Cargo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarUsuarioLojistaRequest(
    @NotBlank(message = "Nome obrigatório") String nome,
    @Email @NotBlank(message = "Email obrigatório") String email,
    @NotBlank(message = "Senha obrigatória") String senha,
    @NotNull(message = "Cargo obrigatório") Cargo cargo) {}
