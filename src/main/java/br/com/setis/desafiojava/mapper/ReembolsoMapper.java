package br.com.setis.desafiojava.mapper;

import br.com.setis.desafiojava.domain.entity.Reembolso;
import br.com.setis.desafiojava.dto.pagamento.ReembolsoResponse;
import br.com.setis.desafiojava.utils.CurrencyFormatter;
import javax.money.MonetaryAmount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReembolsoMapper {
  @Mapping(target = "valorFormatado", source = "valor", qualifiedByName = "formatarMoeda")
  @Mapping(target = "dataSolicitacao", source = "dataCriacao")
  ReembolsoResponse toDto(Reembolso reembolso);

  @Named("formatarMoeda")
  default String formatarMoeda(MonetaryAmount valor) {
    return CurrencyFormatter.format(valor);
  }
}
