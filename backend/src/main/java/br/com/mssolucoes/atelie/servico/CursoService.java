package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.*;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.*;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Cursos online gravados. A aluna compra pelo site (Pix manual, como nas oficinas), a Lenne confere e
 * libera; a partir daí o link com o código da matrícula abre as aulas e guarda o progresso.
 * Os links dos vídeos só saem do servidor para quem tem o curso liberado (ou nas aulas gratuitas).
 */
@Service
public class CursoService {

    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F-]{36}");
    private static final Set<StatusMatricula> PENDENTES = EnumSet.of(StatusMatricula.AGUARDANDO_PIX, StatusMatricula.A_CONFERIR);

    private final CursoRepository cursos;
    private final CursoAulaRepository aulas;
    private final MatriculaRepository matriculas;
    private final MatriculaAulaRepository concluidas;
    private final LancamentoRepository lancamentos;
    private final ClienteService clientes;
    private final ConfiguracaoService configuracao;
    private final ArmazenamentoService armazenamento;

    public CursoService(CursoRepository cursos, CursoAulaRepository aulas, MatriculaRepository matriculas,
                        MatriculaAulaRepository concluidas, LancamentoRepository lancamentos, ClienteService clientes,
                        ConfiguracaoService configuracao, ArmazenamentoService armazenamento) {
        this.cursos = cursos;
        this.aulas = aulas;
        this.matriculas = matriculas;
        this.concluidas = concluidas;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
        this.configuracao = configuracao;
        this.armazenamento = armazenamento;
    }

    // ---------- Cursos (sistema) ----------

    @Transactional(readOnly = true)
    public List<CursoResp> listar() {
        return cursos.findAllByOrderByOrdemAscIdDesc().stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public CursoResp detalhar(Long id) {
        return paraResp(buscar(id));
    }

    @Transactional
    public CursoResp criar(CursoReq r) {
        Curso c = new Curso();
        preencher(c, r);
        return paraResp(cursos.save(c));
    }

    @Transactional
    public CursoResp atualizar(Long id, CursoReq r) {
        Curso c = buscar(id);
        preencher(c, r);
        return paraResp(cursos.save(c));
    }

    @Transactional
    public void excluir(Long id) {
        Curso c = buscar(id);
        if (matriculas.existsByCursoId(id)) {
            throw new RegraNegocioException("Este curso já tem alunas. Despublique em vez de excluir.");
        }
        cursos.delete(c);
        if (c.getCapa() != null) {
            armazenamento.excluir(c.getCapa());
        }
    }

    @Transactional
    public CursoResp trocarCapa(Long id, MultipartFile foto) {
        Curso c = buscar(id);
        String antiga = c.getCapa();
        c.setCapa(armazenamento.salvarImagem(foto));
        cursos.save(c);
        if (antiga != null) {
            armazenamento.excluir(antiga);
        }
        return paraResp(c);
    }

    // ---------- Aulas ----------

    @Transactional
    public CursoResp criarAula(Long cursoId, AulaReq r) {
        Curso c = buscar(cursoId);
        CursoAula a = new CursoAula();
        a.setCurso(c);
        preencher(a, r);
        a.setOrdem(c.getAulas().stream().mapToInt(CursoAula::getOrdem).max().orElse(0) + 1);
        c.getAulas().add(a);
        return paraResp(cursos.save(c));
    }

    @Transactional
    public CursoResp atualizarAula(Long aulaId, AulaReq r) {
        CursoAula a = buscarAula(aulaId);
        preencher(a, r);
        aulas.save(a);
        return paraResp(a.getCurso());
    }

    @Transactional
    public CursoResp excluirAula(Long aulaId) {
        CursoAula a = buscarAula(aulaId);
        Curso c = a.getCurso();
        c.getAulas().remove(a);
        return paraResp(cursos.save(c));
    }

    /** Recebe os ids das aulas na nova ordem. */
    @Transactional
    public CursoResp reordenar(Long cursoId, List<Long> ids) {
        Curso c = buscar(cursoId);
        Set<Long> doCurso = new HashSet<>(c.getAulas().stream().map(CursoAula::getId).toList());
        if (!doCurso.equals(new HashSet<>(ids)) || ids.size() != doCurso.size()) {
            throw new RegraNegocioException("A lista de aulas não confere com as aulas do curso.");
        }
        for (CursoAula a : c.getAulas()) {
            a.setOrdem(ids.indexOf(a.getId()) + 1);
        }
        c.getAulas().sort(Comparator.comparingInt(CursoAula::getOrdem));
        return paraResp(cursos.save(c));
    }

    // ---------- Matrículas (sistema) ----------

    @Transactional(readOnly = true)
    public List<MatriculaResp> matriculas(Long cursoId) {
        Curso c = buscar(cursoId);
        return matriculas.findByCursoIdOrderByCriadoEmDesc(cursoId).stream().map(m -> paraResp(m, c)).toList();
    }

    @Transactional(readOnly = true)
    public List<MatriculaResp> pendentes() {
        return matriculas.findByStatusInOrderByCriadoEmAsc(PENDENTES).stream()
                .map(m -> paraResp(m, m.getCurso())).toList();
    }

    @Transactional
    public MatriculaResp matricularManual(Long cursoId, MatriculaReq r) {
        Curso c = buscar(cursoId);
        Matricula m = nova(c, r.nome(), r.whatsapp(), r.email(), Origem.MANUAL);
        StatusMatricula desejado = r.status() == null ? StatusMatricula.AGUARDANDO_PIX : r.status();
        aplicarStatus(m, desejado, FormaPagamento.PIX);
        return paraResp(m, c);
    }

    @Transactional
    public MatriculaResp mudarStatus(Long id, StatusMatriculaReq r) {
        Matricula m = matriculas.findById(id).orElseThrow(() -> new NaoEncontradoException("Matrícula"));
        aplicarStatus(m, r.status(), r.formaPagamento());
        return paraResp(m, m.getCurso());
    }

    /** Liberar = acesso às aulas, cliente cadastrada e entrada no financeiro. Desfazer tira a entrada. */
    private void aplicarStatus(Matricula m, StatusMatricula novo, FormaPagamento forma) {
        StatusMatricula antigo = m.getStatus();
        if (antigo == novo && m.getId() != null) {
            return;
        }
        if (antigo == StatusMatricula.LIBERADA && novo != StatusMatricula.LIBERADA) {
            lancamentos.deleteByMatriculaId(m.getId());
            m.setLiberadaEm(null);
        }
        m.setStatus(novo);
        matriculas.save(m);
        if (novo == StatusMatricula.LIBERADA) {
            Curso c = m.getCurso();
            if (m.getCliente() == null) {
                m.setCliente(clientes.encontrarOuCriar(m.getNome(), m.getWhatsapp(), TipoCliente.ALUNA));
            }
            m.setLiberadaEm(LocalDateTime.now());
            if (c.getValor().signum() > 0 && !lancamentos.existsByMatriculaId(m.getId())) {
                Lancamento l = new Lancamento();
                l.setTipo(TipoLancamento.ENTRADA);
                l.setCategoria(CategoriaLancamento.CURSO);
                l.setDescricao(("Curso online — " + c.getTitulo() + " (" + m.getNome() + ")"));
                l.setValor(c.getValor());
                l.setData(LocalDate.now());
                l.setFormaPagamento(forma == null ? FormaPagamento.PIX : forma);
                l.setMatricula(m);
                lancamentos.save(l);
            }
            matriculas.save(m);
        }
    }

    // ---------- Site ----------

    @Transactional(readOnly = true)
    public List<CursoPublico> publicados() {
        return cursos.findByPublicadoTrueOrderByOrdemAscIdDesc().stream()
                .map(c -> publico(c, false, Set.of())).toList();
    }

    @Transactional(readOnly = true)
    public CursoPublico publicado(Long id) {
        Curso c = buscar(id);
        if (!c.isPublicado()) {
            throw new NaoEncontradoException("Curso");
        }
        return publico(c, false, Set.of());
    }

    @Transactional
    public MatriculaPublicaResp comprarPeloSite(Long cursoId, MatriculaPublicaReq r) {
        if (Texto.limpar(r.site()) != null) {
            throw new RegraNegocioException("Não foi possível concluir a inscrição.");
        }
        Curso c = buscar(cursoId);
        if (!c.isPublicado()) {
            throw new RegraNegocioException("Este curso não está disponível no momento.");
        }
        String fone = Texto.whatsapp(r.whatsapp());
        if (matriculas.existsByCursoIdAndWhatsappAndStatusNot(cursoId, fone, StatusMatricula.CANCELADA)) {
            throw new RegraNegocioException("Já existe uma inscrição com este WhatsApp neste curso. "
                    + "Fale com a gente pelo WhatsApp que enviamos o seu link de acesso.");
        }
        Matricula m = nova(c, r.nome(), r.whatsapp(), r.email(), Origem.SITE);
        if (c.getValor().signum() == 0) {
            // Curso gratuito: libera na hora
            aplicarStatus(m, StatusMatricula.LIBERADA, FormaPagamento.PIX);
        }
        Configuracao cfg = configuracao.atual();
        return new MatriculaPublicaResp(m.getCodigo(), m.getStatus(), c.getTitulo(), c.getValor(), cfg.getChavePix(),
                cfg.getTitularPix(), cfg.getWhatsapp());
    }

    @Transactional
    public AreaAluna avisarComprovante(String codigo) {
        Matricula m = porCodigo(codigo);
        if (m.getStatus() == StatusMatricula.AGUARDANDO_PIX) {
            m.setStatus(StatusMatricula.A_CONFERIR);
            matriculas.save(m);
        }
        return area(m);
    }

    @Transactional(readOnly = true)
    public AreaAluna areaDaAluna(String codigo) {
        return area(porCodigo(codigo));
    }

    @Transactional
    public AreaAluna marcarAula(String codigo, Long aulaId, boolean concluida) {
        Matricula m = porCodigo(codigo);
        if (m.getStatus() != StatusMatricula.LIBERADA) {
            throw new RegraNegocioException("O curso ainda não foi liberado.");
        }
        boolean doCurso = m.getCurso().getAulas().stream().anyMatch(a -> a.getId().equals(aulaId));
        if (!doCurso) {
            throw new NaoEncontradoException("Aula");
        }
        Optional<MatriculaAula> atual = concluidas.findByMatriculaIdAndAulaId(m.getId(), aulaId);
        if (concluida && atual.isEmpty()) {
            MatriculaAula ma = new MatriculaAula();
            ma.setMatricula(m);
            ma.setAulaId(aulaId);
            concluidas.save(ma);
        } else if (!concluida) {
            atual.ifPresent(concluidas::delete);
        }
        concluidas.flush();
        return area(m);
    }

    // ---------- Apoio ----------

    private Matricula nova(Curso c, String nome, String whatsapp, String email, Origem origem) {
        Matricula m = new Matricula();
        m.setCurso(c);
        m.setNome(nome.trim());
        m.setWhatsapp(Texto.whatsapp(whatsapp));
        m.setEmail(Texto.limpar(email));
        m.setOrigem(origem);
        return matriculas.save(m);
    }

    private Matricula porCodigo(String codigo) {
        if (codigo == null || !UUID.matcher(codigo).matches()) {
            throw new NaoEncontradoException("Matrícula");
        }
        Matricula m = matriculas.findByCodigo(codigo).orElseThrow(() -> new NaoEncontradoException("Matrícula"));
        if (m.getStatus() == StatusMatricula.CANCELADA) {
            throw new NaoEncontradoException("Matrícula");
        }
        return m;
    }

    private AreaAluna area(Matricula m) {
        Set<Long> feitas = new HashSet<>();
        concluidas.findByMatriculaId(m.getId()).forEach(ma -> feitas.add(ma.getAulaId()));
        Configuracao cfg = configuracao.atual();
        boolean liberada = m.getStatus() == StatusMatricula.LIBERADA;
        return new AreaAluna(m.getNome(), m.getStatus(), publico(m.getCurso(), liberada, feitas), feitas.size(),
                m.getCurso().getValor(), liberada ? null : cfg.getChavePix(), liberada ? null : cfg.getTitularPix(),
                cfg.getWhatsapp());
    }

    private CursoPublico publico(Curso c, boolean liberado, Set<Long> feitas) {
        List<AulaPublica> lista = c.getAulas().stream().map(a -> new AulaPublica(a.getId(), a.getTitulo(),
                a.getDescricao(), a.getDuracaoMinutos(), a.isGratuita(),
                liberado || a.isGratuita() ? VideoLink.embed(a.getVideoUrl()) : null,
                feitas.contains(a.getId()))).toList();
        return new CursoPublico(c.getId(), c.getTitulo(), c.getResumo(), c.getDescricao(), url(c.getCapa()),
                c.getValor(), c.getNivel(), c.getMateriais(), lista.size(), duracao(c), lista);
    }

    private Curso buscar(Long id) {
        return cursos.findById(id).orElseThrow(() -> new NaoEncontradoException("Curso"));
    }

    private CursoAula buscarAula(Long id) {
        return aulas.findById(id).orElseThrow(() -> new NaoEncontradoException("Aula"));
    }

    private static void preencher(Curso c, CursoReq r) {
        c.setTitulo(r.titulo().trim());
        c.setResumo(Texto.limpar(r.resumo()));
        c.setDescricao(Texto.limpar(r.descricao()));
        c.setValor(r.valor());
        c.setNivel(Texto.limpar(r.nivel()));
        c.setMateriais(Texto.limpar(r.materiais()));
        c.setPublicado(Boolean.TRUE.equals(r.publicado()));
        c.setOrdem(r.ordem() == null ? 0 : r.ordem());
    }

    private static void preencher(CursoAula a, AulaReq r) {
        String link = Texto.limpar(r.videoUrl());
        if (link != null && VideoLink.embed(link) == null) {
            throw new RegraNegocioException("Use um link do YouTube (pode ser não listado), do Vimeo ou do Google Drive.");
        }
        a.setTitulo(r.titulo().trim());
        a.setDescricao(Texto.limpar(r.descricao()));
        a.setVideoUrl(link);
        a.setDuracaoMinutos(r.duracaoMinutos());
        a.setGratuita(Boolean.TRUE.equals(r.gratuita()));
    }

    private static int duracao(Curso c) {
        return c.getAulas().stream().map(CursoAula::getDuracaoMinutos).filter(Objects::nonNull).mapToInt(i -> i).sum();
    }

    private static String url(String arquivo) {
        return arquivo == null ? null : "/uploads/" + arquivo;
    }

    private CursoResp paraResp(Curso c) {
        List<AulaResp> lista = c.getAulas().stream().map(a -> new AulaResp(a.getId(), a.getTitulo(), a.getDescricao(),
                a.getVideoUrl(), VideoLink.embed(a.getVideoUrl()), a.getDuracaoMinutos(), a.isGratuita(), a.getOrdem())).toList();
        long liberadas = c.getId() == null ? 0 : matriculas.countByCursoIdAndStatus(c.getId(), StatusMatricula.LIBERADA);
        long pendentes = c.getId() == null ? 0 : matriculas.countByCursoIdAndStatusIn(c.getId(), PENDENTES);
        BigDecimal receita = c.getId() == null ? BigDecimal.ZERO : lancamentos.receitaCurso(c.getId());
        return new CursoResp(c.getId(), c.getTitulo(), c.getResumo(), c.getDescricao(), url(c.getCapa()), c.getValor(),
                c.getNivel(), c.getMateriais(), c.isPublicado(), c.getOrdem(), lista, duracao(c), liberadas, pendentes, receita);
    }

    private MatriculaResp paraResp(Matricula m, Curso c) {
        return new MatriculaResp(m.getId(), c.getId(), c.getTitulo(), m.getCliente() == null ? null : m.getCliente().getId(),
                m.getNome(), m.getWhatsapp(), m.getEmail(), m.getStatus(), m.getOrigem(), m.getCodigo(),
                (int) concluidas.countByMatriculaId(m.getId()), c.getAulas().size(), m.getLiberadaEm(), m.getCriadoEm());
    }
}
