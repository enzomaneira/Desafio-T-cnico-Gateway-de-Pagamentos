package br.com.setis.desafiojava.dto.lojista;

import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaInicial;
import br.com.setis.desafiojava.dto.validation.Cnpj;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarLojistaRequest(
    @NotBlank(message = "CNPJ obrigatório")
        @Size(min = 18, max = 18, message = "Tamanho de CNPJ inválido")
        @Cnpj
        String cnpj,
    @NotBlank(message = "Nome Fantasia obrigatório") String nomeFantasia,
    @NotNull(message = "Informações do primeiro usuário obrigatórias") @Valid
        CriarUsuarioLojistaInicial usuarioInicial) {}
