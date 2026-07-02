package br.com.setis.desafiojava.dto.pagamento;

import br.com.setis.desafiojava.domain.entity.StatusReembolso;
import java.time.LocalDateTime;

public record ReembolsoResponse(
    String id, String valorFormatado, StatusReembolso status, LocalDateTime dataSolicitacao) {}
