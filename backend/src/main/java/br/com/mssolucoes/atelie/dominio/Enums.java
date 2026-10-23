package br.com.mssolucoes.atelie.dominio;

/** Enumerações do domínio, agrupadas para facilitar a leitura. */
public final class Enums {

    private Enums() {
    }

    public enum TipoCliente { CLIENTE, NOIVA, PARCEIRO, ALUNA }

    public enum StatusOrcamento { NOVO, RESPONDIDO, APROVADO, RECUSADO }

    public enum Origem { SITE, MANUAL }

    /** Frente de receita de uma encomenda. */
    public enum Frente { ENCOMENDA, CASAMENTO, PRESENTE }

    public enum StatusEncomenda { AGUARDANDO_SINAL, EM_PRODUCAO, PRONTA, ENTREGUE, CANCELADA }

    public enum StatusOficina { ABERTA, ENCERRADA, CANCELADA }

    public enum StatusInscricao { AGUARDANDO_PIX, A_CONFERIR, CONFIRMADA, LISTA_ESPERA, CANCELADA }

    public enum TipoLancamento { ENTRADA, SAIDA }

    public enum CategoriaLancamento { ENCOMENDA, CASAMENTO, PRESENTE, OFICINA, VENDA, MATERIAL, OUTROS }

    /** De onde veio a saída de material. */
    public enum OrigemConsumo { FICHA_ENCOMENDA, MANUAL_ENCOMENDA, PRODUCAO_ESTOQUE }

    public enum CanalVenda { ATELIE, FEIRA, INSTAGRAM, WHATSAPP, OUTRO }

    public enum StatusVenda { CONCLUIDA, CANCELADA }

    public enum FormaPagamento { PIX, DINHEIRO, CARTAO, OUTRO }

    public enum CategoriaPortfolio { DECORACAO, CASAMENTOS, PRESENTES, OFICINAS }
}
