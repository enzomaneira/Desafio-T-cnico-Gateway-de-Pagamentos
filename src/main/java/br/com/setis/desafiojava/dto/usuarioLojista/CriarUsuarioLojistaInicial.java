package br.com.setis.desafiojava.dto.usuarioLojista;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CriarUsuarioLojistaInicial(
    @NotBlank(message = "Nome obrigatório") String nome,
    @Email @NotBlank(message = "Email obrigatório") String email,
    @NotBlank String senha) {}
