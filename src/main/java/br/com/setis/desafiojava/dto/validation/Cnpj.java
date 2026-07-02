package br.com.setis.desafiojava.dto.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * O elemento anotado deve possuir CNPJ Alfanumérico válido
 *
 * @author Stefano Giordano
 */
@Documented
@Constraint(validatedBy = CnpjValidador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cnpj {
  String message() default "CNPJ inválido";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
