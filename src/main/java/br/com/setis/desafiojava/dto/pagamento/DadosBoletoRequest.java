package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.Provedor;
import br.com.setis.desafiojava.dto.validation.CpfOuCnpj;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record DadosBoletoRequest(
    @NotBlank(message = "Documento do pagador obrigatório") @CpfOuCnpj String documentoPagador,
    @Email @NotBlank(message = "Email do pagador obrigatório") String emailPagador,
    @NotNull(message = "Data de vencimento obrigatório")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataVencimento,
    @NotNull(message = "Provedor obrigatório") Provedor provedor)
    implements DadosPagamentoRequest {}
