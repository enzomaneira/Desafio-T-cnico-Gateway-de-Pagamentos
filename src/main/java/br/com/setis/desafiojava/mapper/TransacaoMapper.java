package br.com.setis.desafiojava.mapper;

import br.com.setis.desafiojava.domain.entity.Transacao;
import br.com.setis.desafiojava.dto.pagamento.TransacaoResponse;
import br.com.setis.desafiojava.utils.CurrencyFormatter;
import javax.money.MonetaryAmount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TransacaoMapper {
  @Mapping(target = "valorFormatado", source = "valor", qualifiedByName = "formatarMoeda")
  @Mapping(target = "cnpjLojista", source = "lojista.cnpj")
  TransacaoResponse toDto(Transacao transacao);

  @Named("formatarMoeda")
  default String formatarMoeda(MonetaryAmount valor) {
    return CurrencyFormatter.format(valor);
  }
}
