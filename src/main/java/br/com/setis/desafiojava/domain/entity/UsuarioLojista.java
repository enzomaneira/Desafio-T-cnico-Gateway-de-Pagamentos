package br.com.setis.desafiojava.domain.entity;

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
@Table(name = "usuarios_lojista")
public class UsuarioLojista {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lojista_id", nullable = false)
  private Lojista lojista;

  @Column(name = "idp_id", nullable = false)
  private String idpId;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Cargo cargo;

  @Builder.Default
  @Column(nullable = false, columnDefinition = "tinyint")
  private boolean ativo = true;

  @Column(name = "ultimo_login")
  private LocalDateTime ultimoLogin;

  @Column(name = "data_criacao", nullable = false)
  private LocalDateTime dataCriacao;

  @Column(name = "data_atualizacao", nullable = false)
  private LocalDateTime dataAtualizacao;

  @PrePersist
  public void prePersist() {
    this.dataCriacao = LocalDateTime.now();
    this.dataAtualizacao = LocalDateTime.now();
    this.ativo = true;
  }

  @PreUpdate
  public void preUpdate() {
    this.dataAtualizacao = LocalDateTime.now();
  }
}
