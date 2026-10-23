package br.com.mssolucoes.atelie;

import br.com.mssolucoes.atelie.servico.VideoLink;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 5: cursos online (compra por Pix, liberação, área da aluna) e catálogo de peças no site. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CursosCatalogoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;

    @BeforeEach
    void preparar() throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teste@atelie.local\",\"senha\":\"senha-teste-123\"}")).andReturn().getResponse().getContentAsString();
        token = json.readTree(r).get("token").asText();
        enviar(put("/api/configuracoes"), """
                {"nomeAtelie":"Lenne Carvalho Ateliê","whatsapp":"98988793891","chavePix":"lenne@pix.com","cidade":"São Luís — MA"}""",
                status().isOk());
    }

    @Test
    void linksDeVideoAceitosViramPlayerEOutrosSaoRecusados() {
        assertThat(VideoLink.embed("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
                .isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?rel=0&modestbranding=1");
        assertThat(VideoLink.embed("https://youtu.be/dQw4w9WgXcQ?si=abc")).startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(VideoLink.embed("https://m.youtube.com/shorts/dQw4w9WgXcQ")).contains("/embed/dQw4w9WgXcQ");
        assertThat(VideoLink.embed("https://vimeo.com/123456789/abcdef1234")).isEqualTo("https://player.vimeo.com/video/123456789?h=abcdef1234");
        assertThat(VideoLink.embed("https://vimeo.com/123456789")).isEqualTo("https://player.vimeo.com/video/123456789");
        assertThat(VideoLink.embed("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp/view?usp=sharing"))
                .isEqualTo("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp/preview");
        assertThat(VideoLink.embed("https://exemplo.com/video.mp4")).isNull();
        assertThat(VideoLink.embed("javascript:alert(1)")).isNull();
        assertThat(VideoLink.embed("https://www.youtube.com/watch?v=<script>")).isNull();
        assertThat(VideoLink.embed("https://youtube.com.golpe.com/watch?v=dQw4w9WgXcQ")).isNull();
    }

    @Test
    void cursoVendidoPeloSiteComPixLiberacaoProgressoELembreteDeAcesso() throws Exception {
        long curso = id(enviar(post("/api/cursos"), """
                {"titulo":"Macramê do zero","resumo":"Seis aulas para fazer sua primeira mandala em casa","valor":97,
                 "nivel":"Iniciante","materiais":"Cordão 5 mm\\nAro de 20 cm","publicado":true}""", status().isCreated()));
        enviar(post("/api/cursos/" + curso + "/aulas"), """
                {"titulo":"Boas-vindas e materiais","videoUrl":"https://youtu.be/dQw4w9WgXcQ","duracaoMinutos":6,"gratuita":true}""",
                status().isCreated());
        enviar(post("/api/cursos/" + curso + "/aulas"), """
                {"titulo":"Nó quadrado","videoUrl":"https://vimeo.com/123456789","duracaoMinutos":18}""", status().isCreated());
        JsonNode c = enviar(post("/api/cursos/" + curso + "/aulas"), """
                {"titulo":"Montando a mandala","videoUrl":"https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp/view","duracaoMinutos":35}""",
                status().isCreated());
        enviar(post("/api/cursos/" + curso + "/aulas"), """
                {"titulo":"Inválida","videoUrl":"https://exemplo.com/aula.mp4"}""", status().isBadRequest());
        assertThat(c.get("duracaoTotal").asInt()).isEqualTo(59);
        long a1 = c.get("aulas").get(0).get("id").asLong();
        long a2 = c.get("aulas").get(1).get("id").asLong();
        long a3 = c.get("aulas").get(2).get("id").asLong();

        // Reordenar: a aula 3 vira a 2
        JsonNode reord = enviar(put("/api/cursos/" + curso + "/ordem-aulas"), "{\"ids\":[%d,%d,%d]}".formatted(a1, a3, a2), status().isOk());
        assertThat(reord.get("aulas").get(1).get("titulo").asText()).isEqualTo("Montando a mandala");
        enviar(put("/api/cursos/" + curso + "/ordem-aulas"), "{\"ids\":[%d,%d]}".formatted(a1, a3), status().isBadRequest());

        // No site: só a aula gratuita tem vídeo
        JsonNode site = publico(get2("/api/publico/site"), status().isOk());
        JsonNode cursoSite = site.get("cursos").get(0);
        assertThat(cursoSite.get("titulo").asText()).isEqualTo("Macramê do zero");
        assertThat(cursoSite.get("aulas").get(0).get("videoEmbed").asText()).contains("youtube-nocookie.com/embed/");
        assertThat(cursoSite.get("aulas").get(1).get("videoEmbed").isNull()).isTrue();
        assertThat(site.toString()).doesNotContain("vimeo.com/123456789").doesNotContain("1AbCdEfGhIjKlMnOp");

        // Compra pelo site
        JsonNode compra = publico(post("/api/publico/cursos/" + curso + "/matriculas")
                .content("{\"nome\":\"Joana Lima\",\"whatsapp\":\"(98) 98111-2222\",\"email\":\"joana@email.com\"}"), status().isCreated());
        assertThat(compra.get("status").asText()).isEqualTo("AGUARDANDO_PIX");
        assertThat(compra.get("chavePix").asText()).isEqualTo("lenne@pix.com");
        String codigo = compra.get("codigo").asText();
        publico(post("/api/publico/cursos/" + curso + "/matriculas")
                .content("{\"nome\":\"Joana\",\"whatsapp\":\"98981112222\"}"), status().isBadRequest());

        JsonNode area = publico(get2("/api/publico/matriculas/" + codigo), status().isOk());
        assertThat(area.get("curso").get("aulas").get(1).get("videoEmbed").isNull()).isTrue();
        publico(put("/api/publico/matriculas/" + codigo + "/aulas/" + a2), status().isBadRequest());
        area = publico(post("/api/publico/matriculas/" + codigo + "/comprovante-enviado"), status().isOk());
        assertThat(area.get("status").asText()).isEqualTo("A_CONFERIR");

        // Lenne confere e libera
        JsonNode pend = get("/api/matriculas/pendentes");
        assertThat(pend).hasSize(1);
        long mat = id(pend.get(0));
        JsonNode lib = enviar(patch("/api/matriculas/" + mat + "/status"), "{\"status\":\"LIBERADA\"}", status().isOk());
        assertThat(lib.get("clienteId").isNull()).isFalse();
        JsonNode lanc = get("/api/financeiro").get("lancamentos").get(0);
        assertThat(lanc.get("categoria").asText()).isEqualTo("CURSO");
        assertThat(lanc.get("valor").decimalValue()).isEqualByComparingTo("97");
        assertThat(lanc.get("automatico").asBoolean()).isTrue();
        enviar(delete("/api/lancamentos/" + id(lanc)), null, status().isBadRequest());
        assertThat(get("/api/cursos/" + curso).get("receita").decimalValue()).isEqualByComparingTo("97");

        // Agora a aluna vê todas as aulas e marca o progresso
        area = publico(get2("/api/publico/matriculas/" + codigo), status().isOk());
        assertThat(area.get("curso").get("aulas").get(2).get("videoEmbed").asText()).startsWith("https://player.vimeo.com/video/");
        assertThat(area.has("chavePix") && !area.get("chavePix").isNull()).isFalse();
        area = publico(put("/api/publico/matriculas/" + codigo + "/aulas/" + a2), status().isOk());
        assertThat(area.get("concluidas").asInt()).isEqualTo(1);
        publico(put("/api/publico/matriculas/" + codigo + "/aulas/" + a2), status().isOk());
        assertThat(get("/api/cursos/" + curso + "/matriculas").get(0).get("aulasConcluidas").asInt()).isEqualTo(1);
        area = publico(put("/api/publico/matriculas/" + codigo + "/aulas/" + a2 + "?concluida=false"), status().isOk());
        assertThat(area.get("concluidas").asInt()).isZero();
        publico(put("/api/publico/matriculas/" + codigo + "/aulas/999999"), status().isNotFound());
        publico(get2("/api/publico/matriculas/abc"), status().isNotFound());

        // Lembrete com o link de acesso
        JsonNode acesso = null;
        for (JsonNode l : get("/api/lembretes")) {
            if (l.get("chave").asText().equals("ACESSO:" + mat)) {
                acesso = l;
            }
        }
        assertThat(acesso).isNotNull();
        assertThat(acesso.get("mensagem").asText()).contains("/meu-curso/" + codigo);

        // Curso com aluna não é excluído; voltar a matrícula tira a entrada e o acesso
        enviar(delete("/api/cursos/" + curso), null, status().isBadRequest());
        enviar(patch("/api/matriculas/" + mat + "/status"), "{\"status\":\"A_CONFERIR\"}", status().isOk());
        assertThat(get("/api/cursos/" + curso).get("receita").decimalValue()).isEqualByComparingTo("0");
        area = publico(get2("/api/publico/matriculas/" + codigo), status().isOk());
        assertThat(area.get("curso").get("aulas").get(2).get("videoEmbed").isNull()).isTrue();

        // Cancelada: o link deixa de abrir
        enviar(patch("/api/matriculas/" + mat + "/status"), "{\"status\":\"CANCELADA\"}", status().isOk());
        publico(get2("/api/publico/matriculas/" + codigo), status().isNotFound());

        // Curso despublicado some do site
        enviar(put("/api/cursos/" + curso), "{\"titulo\":\"Macramê do zero\",\"valor\":97,\"publicado\":false}", status().isOk());
        publico(get2("/api/publico/cursos/" + curso), status().isNotFound());
        publico(post("/api/publico/cursos/" + curso + "/matriculas")
                .content("{\"nome\":\"Bia\",\"whatsapp\":\"98981113333\"}"), status().isBadRequest());
    }

    @Test
    void cursoGratuitoLiberaNaHoraSemLancamento() throws Exception {
        long curso = id(enviar(post("/api/cursos"), "{\"titulo\":\"Aula grátis de chaveiro\",\"valor\":0,\"publicado\":true}", status().isCreated()));
        enviar(post("/api/cursos/" + curso + "/aulas"), "{\"titulo\":\"Chaveiro\",\"videoUrl\":\"https://youtu.be/dQw4w9WgXcQ\"}", status().isCreated());
        JsonNode compra = publico(post("/api/publico/cursos/" + curso + "/matriculas")
                .content("{\"nome\":\"Lia\",\"whatsapp\":\"98981114444\"}"), status().isCreated());
        assertThat(compra.get("status").asText()).isEqualTo("LIBERADA");
        assertThat(get("/api/financeiro").get("lancamentos")).isEmpty();
    }

    @Test
    void catalogoMostraPecasEscolhidasComFotosEPrecoSemCustos() throws Exception {
        long cordao = id(enviar(post("/api/materiais"),
                "{\"nome\":\"Cordão\",\"unidade\":\"metro\",\"quantidade\":100,\"estoqueMinimo\":0,\"custoUnitario\":0.5}", status().isCreated()));
        long mandala = id(enviar(post("/api/produtos"), """
                {"nome":"Mandala 40 cm","frente":"ENCOMENDA","horasMaoObra":3,"precoVenda":180,"materiais":[{"materialId":%d,"quantidade":40}]}
                """.formatted(cordao), status().isCreated()));
        long chaveiro = id(enviar(post("/api/produtos"), """
                {"nome":"Chaveiro","frente":"PRESENTE","horasMaoObra":0.5,"precoVenda":25,"prontaEntrega":true,"estoque":7}""",
                status().isCreated()));
        enviar(post("/api/produtos"), "{\"nome\":\"Fora do site\",\"frente\":\"ENCOMENDA\",\"horasMaoObra\":1}", status().isCreated());

        JsonNode info = enviar(put("/api/produtos/" + mandala + "/catalogo"),
                "{\"noCatalogo\":true,\"textoCatalogo\":\"Mandala feita à mão nas cores que você escolher.\",\"prazoProducao\":\"Até 15 dias\",\"ordemCatalogo\":2}",
                status().isOk());
        assertThat(info.get("noCatalogo").asBoolean()).isTrue();
        enviar(put("/api/produtos/" + chaveiro + "/catalogo"), "{\"noCatalogo\":true,\"ordemCatalogo\":1}", status().isOk());

        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        foto(mandala, png);
        JsonNode comDuas = foto(mandala, png);
        assertThat(comDuas.get("fotos")).hasSize(2);
        long segunda = comDuas.get("fotos").get(1).get("id").asLong();
        String urlSegunda = comDuas.get("fotos").get(1).get("url").asText();
        JsonNode capa = enviar(post("/api/fotos-produto/" + segunda + "/capa"), null, status().isOk());
        assertThat(capa.get("fotos").get(0).get("url").asText()).isEqualTo(urlSegunda);
        assertThat(get("/api/produtos").findValues("catalogo").stream().anyMatch(n -> n.get("fotos").size() == 2)).isTrue();

        JsonNode cat = publico(get2("/api/publico/catalogo"), status().isOk());
        List<String> nomes = new ArrayList<>();
        cat.forEach(i -> nomes.add(i.get("nome").asText()));
        assertThat(nomes).containsExactly("Chaveiro", "Mandala 40 cm");
        JsonNode m = cat.get(1);
        assertThat(m.get("preco").decimalValue()).isEqualByComparingTo("180");
        assertThat(m.get("texto").asText()).startsWith("Mandala feita à mão");
        assertThat(m.get("prazoProducao").asText()).isEqualTo("Até 15 dias");
        assertThat(m.get("fotos").get(0).asText()).isEqualTo(urlSegunda);
        assertThat(cat.get(0).get("disponiveis").asInt()).isEqualTo(7);
        assertThat(cat.toString()).doesNotContain("custo").doesNotContain("margem").doesNotContain("materiais");

        // Preço sob consulta e peça inativa
        enviar(put("/api/produtos/" + mandala + "/catalogo"), "{\"noCatalogo\":true,\"mostrarPreco\":false,\"ordemCatalogo\":2}", status().isOk());
        assertThat(publico(get2("/api/publico/catalogo"), status().isOk()).get(1).get("preco").isNull()).isTrue();
        enviar(put("/api/produtos/" + chaveiro), "{\"nome\":\"Chaveiro\",\"frente\":\"PRESENTE\",\"horasMaoObra\":0.5,\"ativo\":false}", status().isOk());
        assertThat(publico(get2("/api/publico/site"), status().isOk()).get("catalogo")).hasSize(1);

        JsonNode semFoto = enviar(delete("/api/fotos-produto/" + segunda), null, status().isOk());
        assertThat(semFoto.get("fotos")).hasSize(1);

        // Sem login não altera o catálogo
        mvc.perform(put("/api/produtos/" + mandala + "/catalogo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Apoio ----------

    private JsonNode foto(long produto, byte[] bytes) throws Exception {
        String r = mvc.perform(multipart("/api/produtos/" + produto + "/fotos")
                        .file(new MockMultipartFile("foto", "f.png", "image/png", bytes))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r);
    }

    private JsonNode publico(MockHttpServletRequestBuilder b, ResultMatcher esperado) throws Exception {
        String r = mvc.perform(b.contentType(MediaType.APPLICATION_JSON)).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private long id(JsonNode n) {
        return n.get("id").asLong();
    }

    private JsonNode get(String url) throws Exception {
        return json.readTree(mvc.perform(auth(MockMvcRequestBuilders.get(url))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder get2(String url) {
        return MockMvcRequestBuilders.get(url);
    }

    private JsonNode enviar(MockHttpServletRequestBuilder b, String corpo, ResultMatcher esperado) throws Exception {
        MockHttpServletRequestBuilder req = auth(b);
        if (corpo != null) {
            req = req.content(corpo);
        }
        String r = mvc.perform(req).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
