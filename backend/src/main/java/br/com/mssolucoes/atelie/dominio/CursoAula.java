package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "curso_aula")
@Getter
@Setter
public class CursoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /** Link do YouTube (não listado), Vimeo ou Google Drive. */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    /** Aberta para quem ainda não comprou. */
    @Column(nullable = false)
    private boolean gratuita;

    @Column(nullable = false)
    private int ordem;
}
