package br.com.setis.desafiojava.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.money.MonetaryAmount;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.javamoney.moneta.Money;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reembolsos")
public class Reembolso {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transacao_id", nullable = false)
  private Transacao transacao;

  @Column(name = "valor_quantia", nullable = false)
  @Setter(AccessLevel.NONE)
  private BigDecimal valorQuantia;

  @Column(name = "valor_moeda", nullable = false, length = 3)
  @Setter(AccessLevel.NONE)
  private String valorMoeda;

  @Column(nullable = false)
  private String motivo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusReembolso status;

  @Column(name = "data_criacao", nullable = false)
  private LocalDateTime dataCriacao;

  @Column(name = "data_atualizacao", nullable = false)
  private LocalDateTime dataAtualizacao;

  @PrePersist
  public void prePersist() {
    this.dataCriacao = LocalDateTime.now();
    this.dataAtualizacao = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.dataAtualizacao = LocalDateTime.now();
  }

  public MonetaryAmount getValor() {
    if (valorQuantia != null && valorMoeda != null) {
      return Money.of(valorQuantia, valorMoeda);
    }
    return null;
  }

  public void setValor(MonetaryAmount valor) {
    if (valor != null) {
      this.valorQuantia = valor.getNumber().numberValue(BigDecimal.class);
      this.valorMoeda = valor.getCurrency().getCurrencyCode();
    }
  }
}
