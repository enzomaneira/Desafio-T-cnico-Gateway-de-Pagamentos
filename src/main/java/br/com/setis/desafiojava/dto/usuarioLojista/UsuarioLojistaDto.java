package br.com.setis.desafiojava.dto.usuarioLojista;

import br.com.setis.desafiojava.domain.entity.Cargo;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioLojistaDto(
    UUID id,
    String nome,
    String email,
    Cargo cargo,
    boolean ativo,
    UUID lojistaId,
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime ultimoLogin,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao) {}
