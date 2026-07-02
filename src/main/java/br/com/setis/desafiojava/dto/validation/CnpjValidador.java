package br.com.setis.desafiojava.dto.validation;

import br.com.setis.desafiojava.utils.CnpjUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidador implements ConstraintValidator<Cnpj, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // anotação not blank já vai validar, verificação abaixo evita apenas null pointer
    if (value == null || value.trim().isEmpty()) {
      return true;
    }

    return CnpjUtils.validar(value);
  }
}
