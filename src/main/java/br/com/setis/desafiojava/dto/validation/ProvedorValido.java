package br.com.setis.desafiojava.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * O elemento anotado deve possuir um "provedor" que tenha suporte ao metodo de pagamento indicado
 * pelo campo "metodo"
 *
 * @author Stefano Giordano
 * @see br.com.setis.desafiojava.domain.entity.Provedor
 */
@Documented
@Constraint(validatedBy = ProvedorValidador.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProvedorValido {
  String message() default "O provedor selecionado não suporta este método de pagamento";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
