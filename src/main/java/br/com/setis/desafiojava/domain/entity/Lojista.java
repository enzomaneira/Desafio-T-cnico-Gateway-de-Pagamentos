package br.com.setis.desafiojava.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
@Table(name = "lojistas")
public class Lojista {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String cnpj;

  @Column(name = "nome_fantasia", nullable = false)
  private String nomeFantasia;

  @Builder.Default
  @Column(nullable = false, columnDefinition = "tinyint")
  private boolean ativo = true;

  @Column(name = "data_criacao", nullable = false)
  private LocalDateTime dataCriacao;

  @Column(name = "data_atualizacao", nullable = false)
  private LocalDateTime dataAtualizacao;

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = Provedor.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "lojista_provedores", joinColumns = @JoinColumn(name = "lojista_id"))
  @Column(name = "provedor")
  @Builder.Default
  private Set<Provedor> provedores = new HashSet<>();

  @PrePersist
  public void prePersist() {
    this.dataCriacao = LocalDateTime.now();
    this.dataAtualizacao = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.dataAtualizacao = LocalDateTime.now();
  }
}
