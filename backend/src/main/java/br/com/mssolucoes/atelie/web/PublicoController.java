package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.seguranca.LimitadorDeRequisicoes;
import br.com.mssolucoes.atelie.servico.ConfiguracaoService;
import br.com.mssolucoes.atelie.servico.OficinaService;
import br.com.mssolucoes.atelie.servico.OrcamentoService;
import br.com.mssolucoes.atelie.servico.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Rotas usadas pela landing page, sem login. */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final ConfiguracaoService configuracao;
    private final OficinaService oficinas;
    private final PortfolioService portfolio;
    private final OrcamentoService orcamentos;
    private final LimitadorDeRequisicoes limitador;
    private final br.com.mssolucoes.atelie.servico.DepoimentoService depoimentos;
    private final br.com.mssolucoes.atelie.servico.CertificadoService certificados;
    private final br.com.mssolucoes.atelie.servico.CursoService cursos;
    private final br.com.mssolucoes.atelie.servico.CatalogoService catalogo;

    public PublicoController(ConfiguracaoService configuracao, OficinaService oficinas, PortfolioService portfolio,
                             OrcamentoService orcamentos, LimitadorDeRequisicoes limitador,
                             br.com.mssolucoes.atelie.servico.DepoimentoService depoimentos,
                             br.com.mssolucoes.atelie.servico.CertificadoService certificados,
                             br.com.mssolucoes.atelie.servico.CursoService cursos,
                             br.com.mssolucoes.atelie.servico.CatalogoService catalogo) {
        this.cursos = cursos;
        this.catalogo = catalogo;
        this.depoimentos = depoimentos;
        this.certificados = certificados;
        this.configuracao = configuracao;
        this.oficinas = oficinas;
        this.portfolio = portfolio;
        this.orcamentos = orcamentos;
        this.limitador = limitador;
    }

    @GetMapping("/site")
    public SitePublico site() {
        return new SitePublico(configuracao.dto(), oficinas.abertasNoSite(), portfolio.publicados(),
                depoimentos.publicados(), cursos.publicados(), catalogo.publico());
    }

    @GetMapping("/catalogo")
    public java.util.List<ItemCatalogo> catalogo() {
        return catalogo.publico();
    }

    @GetMapping("/cursos/{id}")
    public CursoPublico curso(@PathVariable Long id) {
        return cursos.publicado(id);
    }

    @PostMapping("/cursos/{id}/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaPublicaResp comprarCurso(@PathVariable Long id, @Valid @RequestBody MatriculaPublicaReq r,
                                             HttpServletRequest req) {
        limitador.verificar(req, "matricula", 5, 60 * 60);
        return cursos.comprarPeloSite(id, r);
    }

    /** Área da aluna, aberta pelo link com o código da matrícula. */
    @GetMapping("/matriculas/{codigo}")
    public AreaAluna areaDaAluna(@PathVariable String codigo, HttpServletRequest req) {
        limitador.verificar(req, "area-aluna", 300, 60 * 60);
        return cursos.areaDaAluna(codigo);
    }

    @PostMapping("/matriculas/{codigo}/comprovante-enviado")
    public AreaAluna comprovanteCurso(@PathVariable String codigo, HttpServletRequest req) {
        limitador.verificar(req, "comprovante", 20, 60 * 60);
        return cursos.avisarComprovante(codigo);
    }

    @PutMapping("/matriculas/{codigo}/aulas/{aulaId}")
    public AreaAluna concluirAula(@PathVariable String codigo, @PathVariable Long aulaId,
                                  @RequestParam(defaultValue = "true") boolean concluida, HttpServletRequest req) {
        limitador.verificar(req, "progresso", 300, 60 * 60);
        return cursos.marcarAula(codigo, aulaId, concluida);
    }

    @PostMapping("/orcamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> pedirOrcamento(@Valid @RequestBody OrcamentoPublicoReq r, HttpServletRequest req) {
        limitador.verificar(req, "orcamento", 5, 60 * 60);
        orcamentos.receberDoSite(r);
        return Map.of("mensagem", "Pedido recebido!");
    }

    @PostMapping("/oficinas/{id}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoPublicaResp inscrever(@PathVariable Long id, @Valid @RequestBody InscricaoPublicaReq r,
                                          HttpServletRequest req) {
        limitador.verificar(req, "inscricao", 5, 60 * 60);
        return oficinas.inscreverPeloSite(id, r);
    }

    /** Página do certificado: confere que ele existe e mostra os dados (serve como verificação). */
    @GetMapping("/certificados/{codigo}")
    public CertificadoPublico certificado(@PathVariable String codigo, HttpServletRequest req) {
        limitador.verificar(req, "certificado", 60, 60 * 60);
        return certificados.verificar(codigo);
    }

    @GetMapping("/certificados/{codigo}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> certificadoPdf(@PathVariable String codigo, HttpServletRequest req) {
        limitador.verificar(req, "certificado-pdf", 30, 60 * 60);
        return EquipeSiteController.pdf(certificados.publico(codigo));
    }

    @PostMapping("/inscricoes/{codigo}/comprovante-enviado")
    public InscricaoPublicaResp comprovanteEnviado(@PathVariable String codigo, HttpServletRequest req) {
        limitador.verificar(req, "comprovante", 20, 60 * 60);
        return oficinas.avisarComprovante(codigo);
    }
}
