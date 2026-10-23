package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.*;
import br.com.mssolucoes.atelie.dominio.Enums.StatusEncomenda;
import br.com.mssolucoes.atelie.dominio.Enums.StatusInscricao;
import br.com.mssolucoes.atelie.dominio.Enums.StatusOficina;
import br.com.mssolucoes.atelie.dto.Dtos.LembreteResp;
import br.com.mssolucoes.atelie.repositorio.*;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Monta os lembretes do dia com a mensagem já escrita. O envio é pelo WhatsApp dela (link wa.me):
 * depois de enviar, ela marca como enviado e o lembrete some.
 */
@Service
public class LembreteService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEE, dd/MM", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH'h'mm", PT_BR);
    private static final Pattern CHAVE_VALIDA = Pattern.compile("[A-Z]+(:[0-9-]+)+");
    /** Cobrança de saldo volta a aparecer depois de alguns dias, se continuar em aberto. */
    private static final int DIAS_ENTRE_COBRANCAS = 7;
    /** Por quantos dias depois da oficina o lembrete do certificado fica na lista. */
    private static final int DIAS_CERTIFICADO = 7;

    private final LembreteRepository repo;
    private final InscricaoRepository inscricoes;
    private final EncomendaRepository encomendas;
    private final LancamentoRepository lancamentos;
    private final ClienteRepository clientes;
    private final ConfiguracaoService configuracao;
    private final UrlPublica url;
    private final MatriculaRepository matriculas;

    public LembreteService(LembreteRepository repo, InscricaoRepository inscricoes, EncomendaRepository encomendas,
                           LancamentoRepository lancamentos, ClienteRepository clientes,
                           ConfiguracaoService configuracao, UrlPublica url, MatriculaRepository matriculas) {
        this.url = url;
        this.matriculas = matriculas;
        this.repo = repo;
        this.inscricoes = inscricoes;
        this.encomendas = encomendas;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
        this.configuracao = configuracao;
    }

    @Transactional(readOnly = true)
    public List<LembreteResp> pendentes() {
        LocalDate hoje = LocalDate.now();
        Configuracao cfg = configuracao.atual();
        List<LembreteResp> todos = new ArrayList<>();

        // Oficinas: hoje/amanhã para quem confirmou; Pix pendente para quem só reservou
        for (Inscricao i : inscricoes.findByStatusInOrderByCriadoEmAsc(
                EnumSet.of(StatusInscricao.CONFIRMADA, StatusInscricao.AGUARDANDO_PIX))) {
            Oficina o = i.getOficina();
            // Depois da oficina: agradecer e mandar o link do certificado (até 7 dias depois)
            if (i.getStatus() == StatusInscricao.CONFIRMADA && o.getStatus() != StatusOficina.CANCELADA
                    && o.getInicio().isBefore(LocalDateTime.now())
                    && !o.getInicio().toLocalDate().isBefore(hoje.minusDays(DIAS_CERTIFICADO))) {
                todos.add(new LembreteResp("CERTIFICADO:" + i.getId(), "CERTIFICADO",
                        "Certificado: " + o.getTitulo(), "Oficina de " + DIA.format(o.getInicio().toLocalDate()),
                        i.getNome(), i.getWhatsapp(),
                        "Oi, " + primeiro(i.getNome()) + "! Obrigada por participar da oficina \"" + o.getTitulo()
                                + "\". Foi muito bom ter você aqui! Seu certificado está neste link: "
                                + url.certificado(i.getCodigo())
                                + " Se quiser, me conta em poucas palavras o que achou da oficina, que eu adoraria "
                                + "colocar no site. " + assinatura(cfg), o.getInicio().toLocalDate()));
                continue;
            }
            if (o.getStatus() != StatusOficina.ABERTA || o.getInicio().isBefore(LocalDateTime.now())) {
                continue;
            }
            LocalDate dia = o.getInicio().toLocalDate();
            String quando = DIA.format(dia) + " às " + HORA.format(o.getInicio());
            if (i.getStatus() == StatusInscricao.CONFIRMADA && !dia.isAfter(hoje.plusDays(1))) {
                String hojeOuAmanha = dia.equals(hoje) ? "hoje" : "amanhã";
                todos.add(new LembreteResp("OFICINA:" + i.getId(), "OFICINA",
                        "Oficina " + hojeOuAmanha + ": " + o.getTitulo(), quando, i.getNome(), i.getWhatsapp(),
                        "Oi, " + primeiro(i.getNome()) + "! Passando para lembrar da oficina \"" + o.getTitulo()
                                + "\" " + hojeOuAmanha + " (" + quando + ")"
                                + (o.getLocal() != null ? ", " + o.getLocal() : "")
                                + ". Seu material já está separado, é só vir! Qualquer coisa, me chama aqui. "
                                + assinatura(cfg), dia));
            } else if (i.getStatus() == StatusInscricao.AGUARDANDO_PIX && !dia.isAfter(hoje.plusDays(10))
                    && i.getCriadoEm().toLocalDate().isBefore(hoje)) {
                todos.add(new LembreteResp("PIX:" + i.getId(), "PIX",
                        "Pix pendente: " + o.getTitulo(), "Vaga reservada para " + quando, i.getNome(), i.getWhatsapp(),
                        "Oi, " + primeiro(i.getNome()) + "! Sua vaga na oficina \"" + o.getTitulo() + "\" (" + quando
                                + ") está reservada, mas ainda não recebi o Pix de " + moeda(o.getValor()) + "."
                                + (cfg.getChavePix() != null ? " A chave é " + cfg.getChavePix() + "." : "")
                                + " Se já pagou, me manda o comprovante por aqui? " + assinatura(cfg), dia));
            }
        }

        for (Encomenda e : encomendas.findByStatusIn(EnumSet.of(StatusEncomenda.EM_PRODUCAO,
                StatusEncomenda.PRONTA, StatusEncomenda.ENTREGUE))) {
            Cliente c = e.getCliente();
            BigDecimal saldo = e.getValorTotal().subtract(lancamentos.totalPagoEncomenda(e.getId()));
            String textoSaldo = saldo.signum() > 0 ? " Fica faltando o saldo de " + moeda(saldo)
                    + (cfg.getChavePix() != null ? ", que pode ser pelo Pix " + cfg.getChavePix() : "") + "." : "";
            if (e.getStatus() == StatusEncomenda.PRONTA) {
                todos.add(new LembreteResp("PRONTA:" + e.getId(), "PRONTA", "Peça pronta: " + e.getDescricao(),
                        saldo.signum() > 0 ? "Saldo de " + moeda(saldo) : "Quitada", c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Sua encomenda (" + e.getDescricao() + ") está pronta!"
                                + textoSaldo + " Quando fica bom para você buscar? " + assinatura(cfg), hoje));
            } else if (e.getStatus() == StatusEncomenda.EM_PRODUCAO && e.getPrazoEntrega() != null
                    && e.getPrazoEntrega().equals(hoje.plusDays(1))) {
                todos.add(new LembreteResp("ENTREGA:" + e.getId() + ":" + e.getPrazoEntrega(), "ENTREGA",
                        "Entrega amanhã: " + e.getDescricao(), "Ainda em produção", c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo certo para a entrega da sua encomenda ("
                                + e.getDescricao() + ") amanhã, " + DIA.format(e.getPrazoEntrega())
                                + "? Me confirma o melhor horário para você. " + assinatura(cfg), e.getPrazoEntrega()));
            } else if (e.getStatus() == StatusEncomenda.ENTREGUE && saldo.signum() <= 0 && e.getEntregueEm() != null
                    && !e.getEntregueEm().isAfter(hoje.minusDays(3)) && !e.getEntregueEm().isBefore(hoje.minusDays(10))) {
                // Alguns dias depois da entrega (e já quitada): pedir um depoimento para o site
                todos.add(new LembreteResp("DEPOIMENTO:" + e.getId(), "DEPOIMENTO", "Pedir depoimento: " + e.getDescricao(),
                        "Entregue em " + DIA.format(e.getEntregueEm()), c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo bem? Espero que esteja gostando da sua peça ("
                                + e.getDescricao() + "). Se puder, me conta em poucas palavras o que achou? Com a sua "
                                + "permissão, coloco no site do ateliê. Obrigada pelo carinho! " + assinatura(cfg), hoje));
            } else if (e.getStatus() == StatusEncomenda.ENTREGUE && saldo.signum() > 0) {
                todos.add(new LembreteResp("SALDO:" + e.getId(), "SALDO", "Saldo em aberto: " + e.getDescricao(),
                        moeda(saldo) + " a receber", c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo bem? Passando para lembrar do saldo de "
                                + moeda(saldo) + " da sua encomenda (" + e.getDescricao() + ")."
                                + (cfg.getChavePix() != null ? " Pode ser pelo Pix " + cfg.getChavePix() + "." : "")
                                + " Obrigada! " + assinatura(cfg), hoje));
            }
        }

        // Cursos online: mandar o link de acesso depois de liberar; lembrar do Pix de quem só se inscreveu
        for (Matricula m : matriculas.findByStatusInOrderByCriadoEmAsc(EnumSet.of(
                br.com.mssolucoes.atelie.dominio.Enums.StatusMatricula.LIBERADA,
                br.com.mssolucoes.atelie.dominio.Enums.StatusMatricula.AGUARDANDO_PIX))) {
            Curso curso = m.getCurso();
            if (m.getStatus() == br.com.mssolucoes.atelie.dominio.Enums.StatusMatricula.LIBERADA) {
                if (m.getLiberadaEm() != null && !m.getLiberadaEm().toLocalDate().isBefore(hoje.minusDays(30))) {
                    todos.add(new LembreteResp("ACESSO:" + m.getId(), "ACESSO", "Acesso ao curso: " + curso.getTitulo(),
                            "Liberado em " + DIA.format(m.getLiberadaEm().toLocalDate()), m.getNome(), m.getWhatsapp(),
                            "Oi, " + primeiro(m.getNome()) + "! Seu acesso ao curso \"" + curso.getTitulo()
                                    + "\" está liberado. É só abrir este link (guarde, ele é só seu): "
                                    + url.base() + "/meu-curso/" + m.getCodigo()
                                    + " Qualquer dúvida nas aulas, me chama aqui. Bons nós! " + assinatura(cfg),
                            m.getLiberadaEm().toLocalDate()));
                }
            } else if (m.getCriadoEm().toLocalDate().isBefore(hoje) && !m.getCriadoEm().toLocalDate().isBefore(hoje.minusDays(15))) {
                todos.add(new LembreteResp("PIXCURSO:" + m.getId(), "PIX", "Pix pendente: curso " + curso.getTitulo(),
                        "Inscrição em " + DIA.format(m.getCriadoEm().toLocalDate()), m.getNome(), m.getWhatsapp(),
                        "Oi, " + primeiro(m.getNome()) + "! Vi que você se inscreveu no curso online \""
                                + curso.getTitulo() + "\", mas ainda não recebi o Pix de " + moeda(curso.getValor()) + "."
                                + (cfg.getChavePix() != null ? " A chave é " + cfg.getChavePix() + "." : "")
                                + " Assim que eu confirmar, te mando o link das aulas. " + assinatura(cfg), hoje));
            }
        }

        MonthDay hojeMd = MonthDay.from(hoje);
        for (Cliente c : clientes.findByAniversarioIsNotNull()) {
            MonthDay md = MonthDay.from(c.getAniversario());
            boolean bissextoCaiHoje = md.getMonthValue() == 2 && md.getDayOfMonth() == 29
                    && !hoje.isLeapYear() && hojeMd.equals(MonthDay.of(2, 28));
            if (md.equals(hojeMd) || bissextoCaiHoje) {
                todos.add(new LembreteResp("ANIV:" + c.getId() + ":" + hoje.getYear(), "ANIVERSARIO",
                        "Aniversário de " + c.getNome(), "Hoje", c.getNome(), c.getWhatsapp(),
                        "Feliz aniversário, " + primeiro(c.getNome()) + "! Que seu novo ano seja leve e cheio de "
                                + "coisas bonitas, feitas com carinho. Um abraço do ateliê! " + assinatura(cfg), hoje));
            }
        }

        Map<String, Lembrete> enviados = repo.findByChaveIn(todos.stream().map(LembreteResp::chave).toList())
                .stream().collect(Collectors.toMap(Lembrete::getChave, Function.identity()));
        return todos.stream().filter(l -> {
            Lembrete env = enviados.get(l.chave());
            if (env == null) {
                return true;
            }
            return l.tipo().equals("SALDO")
                    && env.getEnviadoEm().toLocalDate().plusDays(DIAS_ENTRE_COBRANCAS).isBefore(hoje.plusDays(1));
        }).sorted(Comparator.comparing(LembreteResp::data)).toList();
    }

    @Transactional(readOnly = true)
    public int quantidade() {
        return pendentes().size();
    }

    /** Enviado ou dispensado: não aparece de novo (a cobrança de saldo volta depois de 7 dias). */
    @Transactional
    public void marcar(String chave) {
        if (!CHAVE_VALIDA.matcher(chave).matches()) {
            throw new RegraNegocioException("Lembrete inválido.");
        }
        Lembrete l = repo.findByChave(chave).orElseGet(() -> {
            Lembrete novo = new Lembrete();
            novo.setChave(chave);
            return novo;
        });
        l.setEnviadoEm(LocalDateTime.now());
        repo.save(l);
    }

    private static String primeiro(String nome) {
        String n = nome == null ? "" : nome.trim();
        int espaco = n.indexOf(' ');
        return espaco > 0 ? n.substring(0, espaco) : n;
    }

    private static String assinatura(Configuracao c) {
        String responsavel = c.getResponsavel() != null ? primeiro(c.getResponsavel()) : "";
        // "Lenne Carvalho Ateliê" já tem o nome dela: não repete
        if (responsavel.isEmpty() || c.getNomeAtelie().toLowerCase().contains(responsavel.toLowerCase())) {
            return "— " + c.getNomeAtelie();
        }
        return "— " + responsavel + ", " + c.getNomeAtelie();
    }

    static String moeda(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(v);
    }
}
