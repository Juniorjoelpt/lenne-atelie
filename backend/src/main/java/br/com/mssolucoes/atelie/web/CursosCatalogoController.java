package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.servico.CatalogoService;
import br.com.mssolucoes.atelie.servico.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Cursos online, matrículas e catálogo de peças (só a administradora). */
@RestController
@RequestMapping("/api")
public class CursosCatalogoController {

    private final CursoService cursos;
    private final CatalogoService catalogo;

    public CursosCatalogoController(CursoService cursos, CatalogoService catalogo) {
        this.cursos = cursos;
        this.catalogo = catalogo;
    }

    // ---------- Cursos ----------

    @GetMapping("/cursos")
    public List<CursoResp> cursos() {
        return cursos.listar();
    }

    @GetMapping("/cursos/{id}")
    public CursoResp curso(@PathVariable Long id) {
        return cursos.detalhar(id);
    }

    @PostMapping("/cursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResp criarCurso(@Valid @RequestBody CursoReq r) {
        return cursos.criar(r);
    }

    @PutMapping("/cursos/{id}")
    public CursoResp atualizarCurso(@PathVariable Long id, @Valid @RequestBody CursoReq r) {
        return cursos.atualizar(id, r);
    }

    @DeleteMapping("/cursos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCurso(@PathVariable Long id) {
        cursos.excluir(id);
    }

    @PostMapping(value = "/cursos/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CursoResp capa(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) {
        return cursos.trocarCapa(id, foto);
    }

    @PostMapping("/cursos/{id}/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResp criarAula(@PathVariable Long id, @Valid @RequestBody AulaReq r) {
        return cursos.criarAula(id, r);
    }

    @PutMapping("/aulas/{id}")
    public CursoResp atualizarAula(@PathVariable Long id, @Valid @RequestBody AulaReq r) {
        return cursos.atualizarAula(id, r);
    }

    @DeleteMapping("/aulas/{id}")
    public CursoResp excluirAula(@PathVariable Long id) {
        return cursos.excluirAula(id);
    }

    @PutMapping("/cursos/{id}/ordem-aulas")
    public CursoResp ordemAulas(@PathVariable Long id, @Valid @RequestBody OrdemReq r) {
        return cursos.reordenar(id, r.ids());
    }

    // ---------- Matrículas ----------

    @GetMapping("/cursos/{id}/matriculas")
    public List<MatriculaResp> matriculas(@PathVariable Long id) {
        return cursos.matriculas(id);
    }

    @PostMapping("/cursos/{id}/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResp matricular(@PathVariable Long id, @Valid @RequestBody MatriculaReq r) {
        return cursos.matricularManual(id, r);
    }

    @GetMapping("/matriculas/pendentes")
    public List<MatriculaResp> matriculasPendentes() {
        return cursos.pendentes();
    }

    @PatchMapping("/matriculas/{id}/status")
    public MatriculaResp statusMatricula(@PathVariable Long id, @Valid @RequestBody StatusMatriculaReq r) {
        return cursos.mudarStatus(id, r);
    }

    // ---------- Catálogo ----------

    @PutMapping("/produtos/{id}/catalogo")
    public CatalogoInfo atualizarCatalogo(@PathVariable Long id, @Valid @RequestBody CatalogoReq r) {
        return catalogo.atualizar(id, r);
    }

    @PostMapping(value = "/produtos/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoInfo enviarFotoProduto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) {
        return catalogo.enviarFoto(id, foto);
    }

    @PostMapping("/fotos-produto/{id}/capa")
    public CatalogoInfo capaProduto(@PathVariable Long id) {
        return catalogo.capa(id);
    }

    @DeleteMapping("/fotos-produto/{id}")
    public CatalogoInfo excluirFotoProduto(@PathVariable Long id) {
        return catalogo.excluirFoto(id);
    }
}
