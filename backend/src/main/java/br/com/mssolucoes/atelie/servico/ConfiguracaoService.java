package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dto.Dtos.ConfiguracaoDto;
import br.com.mssolucoes.atelie.dto.Dtos.DocumentosConfig;
import br.com.mssolucoes.atelie.repositorio.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConfiguracaoService {

    private final ConfiguracaoRepository repo;

    public ConfiguracaoService(ConfiguracaoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Configuracao atual() {
        return repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoDto dto() {
        return paraDto(atual());
    }

    @Transactional
    public ConfiguracaoDto salvar(ConfiguracaoDto d) {
        Configuracao c = repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
        c.setNomeAtelie(d.nomeAtelie().trim());
        c.setWhatsapp(Texto.whatsapp(d.whatsapp()));
        c.setInstagram(Texto.limpar(d.instagram()) == null ? null : d.instagram().trim().replaceFirst("^@", ""));
        c.setChavePix(Texto.limpar(d.chavePix()));
        c.setTitularPix(Texto.limpar(d.titularPix()));
        c.setEndereco(Texto.limpar(d.endereco()));
        c.setHorario(Texto.limpar(d.horario()));
        c.setCidade(Texto.limpar(d.cidade()));
        c.setSobreTexto(Texto.limpar(d.sobreTexto()));
        c.setAtualizadoEm(LocalDateTime.now());
        return paraDto(repo.save(c));
    }

    public static final String CONDICOES_PADRAO =
            "O pagamento é feito com um sinal para confirmar a encomenda e o restante na entrega, por Pix, dinheiro "
            + "ou cartão. O prazo de produção começa a contar depois do pagamento do sinal. Por serem feitas à mão, "
            + "as peças podem ter pequenas variações em relação às fotos de referência.";

    public static final String CANCELAMENTO_PADRAO =
            "O sinal confirma a encomenda e reserva a data. Em caso de desistência, o sinal não é devolvido, pois "
            + "cobre os materiais comprados e o trabalho já iniciado. Cancelamentos com menos de 15 dias da data "
            + "de entrega podem ter cobrança proporcional ao que já foi produzido. Mudanças de modelo, medida ou cor "
            + "depois do início da produção podem alterar o valor e o prazo.";

    @Transactional(readOnly = true)
    public DocumentosConfig documentos() {
        Configuracao c = atual();
        return new DocumentosConfig(c.getResponsavel(), c.getDocumento(), c.getSinalPercentual(),
                c.getValidadePropostaDias(),
                c.getCondicoesProposta() != null ? c.getCondicoesProposta() : CONDICOES_PADRAO,
                c.getPoliticaCancelamento() != null ? c.getPoliticaCancelamento() : CANCELAMENTO_PADRAO);
    }

    @Transactional
    public DocumentosConfig salvarDocumentos(DocumentosConfig d) {
        Configuracao c = repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
        c.setResponsavel(Texto.limpar(d.responsavel()));
        c.setDocumento(Texto.limpar(d.documento()));
        c.setSinalPercentual(d.sinalPercentual());
        c.setValidadePropostaDias(d.validadePropostaDias());
        c.setCondicoesProposta(Texto.limpar(d.condicoesProposta()));
        c.setPoliticaCancelamento(Texto.limpar(d.politicaCancelamento()));
        c.setAtualizadoEm(LocalDateTime.now());
        repo.save(c);
        return documentos();
    }

    public static Configuracao padrao() {
        Configuracao c = new Configuracao();
        c.setNomeAtelie("Lenne Carvalho Ateliê");
        c.setWhatsapp("5598988793891");
        c.setInstagram("lennecarvalhoatelie");
        c.setCidade("São Luís — MA");
        c.setSobreTexto("Artista têxtil em São Luís e instrutora de macramê. Cada peça nasce de uma conversa: "
                + "entendo o espaço, a ocasião e a pessoa, e transformo isso em fios.");
        return c;
    }

    public static ConfiguracaoDto paraDto(Configuracao c) {
        return new ConfiguracaoDto(c.getNomeAtelie(), c.getWhatsapp(), c.getInstagram(), c.getChavePix(),
                c.getTitularPix(), c.getEndereco(), c.getHorario(), c.getCidade(), c.getSobreTexto());
    }
}
