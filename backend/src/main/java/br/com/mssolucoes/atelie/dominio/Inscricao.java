package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.Origem;
import br.com.mssolucoes.atelie.dominio.Enums.StatusInscricao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscricao")
@Getter
@Setter
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Código público (UUID) usado pela aluna para avisar que enviou o Pix. */
    @Column(nullable = false, length = 36, unique = true)
    private String codigo = java.util.UUID.randomUUID().toString();

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusInscricao status = StatusInscricao.AGUARDANDO_PIX;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Origem origem = Origem.MANUAL;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
