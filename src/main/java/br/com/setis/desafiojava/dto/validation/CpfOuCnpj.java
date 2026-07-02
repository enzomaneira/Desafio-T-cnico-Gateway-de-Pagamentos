package br.com.setis.desafiojava.dto.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.validator.constraints.CompositionType;
import org.hibernate.validator.constraints.ConstraintComposition;
import org.hibernate.validator.constraints.br.CPF;

/**
 * O elemento anotado deve possuir ou um CPF ou um CNPJ Alfanumérico válido
 *
 * @author Stefano Giordano
 */
@ConstraintComposition(CompositionType.OR)
@CPF
@Cnpj
@ReportAsSingleViolation
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface CpfOuCnpj {

  String message() default "Documento inválido";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
