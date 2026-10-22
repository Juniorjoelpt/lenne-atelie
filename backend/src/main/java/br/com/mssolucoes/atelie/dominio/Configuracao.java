package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Dados do ateliê exibidos no site (registro único, id = 1). */
@Entity
@Table(name = "configuracao")
@Getter
@Setter
public class Configuracao {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(name = "nome_atelie", nullable = false, length = 120)
    private String nomeAtelie;

    /** Somente dígitos, com DDI: 5598988793891 */
    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Column(length = 80)
    private String instagram;

    @Column(name = "chave_pix", length = 140)
    private String chavePix;

    @Column(name = "titular_pix", length = 120)
    private String titularPix;

    @Column(length = 255)
    private String endereco;

    @Column(length = 255)
    private String horario;

    @Column(length = 120)
    private String cidade;

    @Column(name = "sobre_texto", columnDefinition = "TEXT")
    private String sobreTexto;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
