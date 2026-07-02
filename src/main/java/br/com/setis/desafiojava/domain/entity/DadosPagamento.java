package br.com.setis.desafiojava.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "dados_pagamento")
public class DadosPagamento {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @JsonIgnore
  private UUID id;

  @Column(name = "numero_cartao_mascarado")
  private String numeroCartaoMascarado;

  @Column(name = "nome_titular")
  private String nomeTitular;

  @Column(name = "codigo_barras_boleto")
  private String codigoBarrasBoleto;

  @Enumerated(EnumType.STRING)
  @Column(name = "provedor")
  private Provedor provedor;

  @Lob
  @Column(name = "pix_qr_code", columnDefinition = "TEXT")
  private String pixQrCodeBase64;

  @Column(name = "pix_chave")
  private String chavePix;

  @Column(name = "data_pagamento")
  private LocalDateTime dataPagamento;

  @Column(name = "data_expiracao")
  private LocalDateTime dataExpiracao;

  @Column(name = "e2e_id")
  private String e2eId;
}
