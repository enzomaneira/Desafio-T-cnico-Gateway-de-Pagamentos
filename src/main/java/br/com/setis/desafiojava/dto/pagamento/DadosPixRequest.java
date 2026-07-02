package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.Provedor;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.hibernate.validator.constraints.UUID;

public record DadosPixRequest(
    // chave pix válidas - Aleatória, CNPJ, Email, Telefone
    @NotBlank(message = "Chave pix do recebedor obrigatória") @UUID String chavePix,
    @NotNull(message = "Data expiração do QR code obrigatória")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataExpiracao,
    @NotNull(message = "Provedor obrigatório") Provedor provedor)
    implements DadosPagamentoRequest {}
