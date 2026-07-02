package br.com.setis.desafiojava.mapper;

import br.com.setis.desafiojava.domain.entity.UsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioLojistaMapper {
  @Mapping(source = "lojista.id", target = "lojistaId")
  UsuarioLojistaDto toDto(UsuarioLojista entity);
}
