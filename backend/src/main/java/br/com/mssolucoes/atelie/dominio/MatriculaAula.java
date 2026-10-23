package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aula que a aluna marcou como concluída. */
@Entity
@Table(name = "matricula_aula")
@Getter
@Setter
public class MatriculaAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Column(name = "aula_id", nullable = false)
    private Long aulaId;

    @Column(name = "concluida_em", nullable = false)
    private LocalDateTime concluidaEm = LocalDateTime.now();
}
