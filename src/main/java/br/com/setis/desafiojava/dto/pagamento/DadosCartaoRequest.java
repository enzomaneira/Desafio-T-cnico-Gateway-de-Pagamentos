package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.Provedor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CreditCardNumber;

public record DadosCartaoRequest(
    @NotBlank
        @Size(min = 16, max = 16, message = "Número do cartão deve ter exatamente 16 dígitos")
        @Pattern(regexp = "\\d+", message = "O número do cartão deve conter apenas dígitos")
        @CreditCardNumber(message = "Número do cartão inválido")
        String numero,
    @NotBlank(message = "Nome do titular do cartão obrigatório") String titular,
    @NotBlank(message = "Validado do cartão obrigatório")
        @Pattern(
            regexp = "^(0[1-9]|1[0-2])/\\d{2}$",
            message = "A validade deve estar no formato MM/YY (ex: 12/28)")
        String validade,
    @NotBlank(message = "CVV do cartão obrigatório")
        @Pattern(regexp = "\\d{3,4}", message = "O CVV deve conter 3 ou 4 dígitos numéricos")
        String cvv,
    @NotNull(message = "Provedor obrigatório") Provedor provedor)
    implements DadosPagamentoRequest {}
