package br.com.setis.desafiojava.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.money.MonetaryAmount;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.javamoney.moneta.Money;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transacoes")
public class Transacao {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lojista_id", nullable = false)
  @ToString.Exclude
  private Lojista lojista;

  @Column(name = "solicitante", nullable = false)
  private String solicitante;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "dados_pagamento_id", nullable = false)
  private DadosPagamento dadosPagamento;

  @OneToMany(mappedBy = "transacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @ToString.Exclude
  @Builder.Default
  private List<Reembolso> reembolsos = new ArrayList<>();

  @Column(name = "valor_quantia", nullable = false)
  @Setter(AccessLevel.NONE)
  private BigDecimal valorQuantia;

  @Column(name = "valor_moeda", nullable = false)
  @Setter(AccessLevel.NONE)
  private String valorMoeda;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MetodoPagamento metodoPagamento;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusTransacao status;

  @Column(name = "id_transacao_psp")
  private String idTransacaoPsp;

  @Lob
  @Column(name = "resposta_psp_pura", columnDefinition = "TEXT")
  private String respostaPspPura;

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

    throw new IllegalStateException("Valor da transação inconsistente (nulo)");
  }

  public void setValor(MonetaryAmount valor) {
    if (valor != null) {
      this.valorQuantia = valor.getNumber().numberValue(BigDecimal.class);
      this.valorMoeda = valor.getCurrency().getCurrencyCode();
      return;
    }

    throw new IllegalArgumentException("MonetaryAmount valor da transação inconsistente (nulo)");
  }
}
