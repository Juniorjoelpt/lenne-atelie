package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.Frente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ficha técnica de uma peça: materiais, horas e outros custos.
 * Se for de pronta-entrega, também guarda quantas peças prontas existem.
 */
@Entity
@Table(name = "produto")
@Getter
@Setter
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frente frente = Frente.ENCOMENDA;

    @Column(name = "horas_mao_obra", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasMaoObra = BigDecimal.ZERO;

    /** Embalagem, tag, argola comprada pronta… por peça. */
    @Column(name = "outros_custos", nullable = false, precision = 12, scale = 2)
    private BigDecimal outrosCustos = BigDecimal.ZERO;

    /** Margem em %; vazio = usa a margem padrão das configurações. */
    @Column(precision = 5, scale = 2)
    private BigDecimal margem;

    /** Preço que ela cobra; vazio = usa o preço sugerido. */
    @Column(name = "preco_venda", precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "pronta_entrega", nullable = false)
    private boolean prontaEntrega;

    @Column(nullable = false)
    private int estoque;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ProdutoMaterial> materiais = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
