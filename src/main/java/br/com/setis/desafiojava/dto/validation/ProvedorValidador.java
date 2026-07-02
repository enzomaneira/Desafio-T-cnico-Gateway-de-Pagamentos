package br.com.setis.desafiojava.dto.validation;

import br.com.setis.desafiojava.dto.pagamento.CriarTransacaoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProvedorValidador
    implements ConstraintValidator<ProvedorValido, CriarTransacaoRequest> {
  @Override
  public boolean isValid(CriarTransacaoRequest request, ConstraintValidatorContext context) {
    if (request == null || request.metodo() == null || request.dadosPagamento() == null) {
      return true;
    }

    var provedor = request.dadosPagamento().provedor();
    var metodo = request.metodo();

    return provedor.suporta(metodo);
  }
}
