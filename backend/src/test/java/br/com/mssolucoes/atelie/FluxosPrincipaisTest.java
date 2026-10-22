package br.com.mssolucoes.atelie;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FluxosPrincipaisTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;

    @BeforeEach
    void entrar() throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teste@atelie.local\",\"senha\":\"senha-teste-123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = json.readTree(r).get("token").asText();
    }

    @Test
    void rotasDeGestaoExigemLogin() throws Exception {
        mvc.perform(get("/api/painel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/publico/site")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teste@atelie.local\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void orcamentoDoSiteViraEncomendaEPagamentoAtualizaSaldo() throws Exception {
        mvc.perform(post("/api/publico/orcamentos").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Ana Paula","whatsapp":"(98) 98111-2233","tipoPeca":"Decoração de casamento ou evento",
                 "medidas":"arco 2m","cores":"cru","dataDesejada":"%s","detalhes":"Cerimônia na praia"}
                """.formatted(LocalDate.now().plusDays(30)))).andExpect(status().isCreated());

        JsonNode lista = getJson("/api/orcamentos");
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("status").asText()).isEqualTo("NOVO");
        assertThat(lista.get(0).get("whatsapp").asText()).isEqualTo("5598981112233");
        long id = lista.get(0).get("id").asLong();

        JsonNode enc = json.readTree(mvc.perform(auth(post("/api/orcamentos/" + id + "/aprovar")).content("""
                {"descricao":"Arco de cerimônia","frente":"CASAMENTO","valorTotal":1200,"valorSinal":400}
                """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(enc.get("status").asText()).isEqualTo("AGUARDANDO_SINAL");
        assertThat(enc.get("saldo").decimalValue()).isEqualByComparingTo("1200");
        long encId = enc.get("id").asLong();

        // não aprova duas vezes
        mvc.perform(auth(post("/api/orcamentos/" + id + "/aprovar")).content("""
                {"descricao":"x","frente":"CASAMENTO","valorTotal":1}
                """)).andExpect(status().isBadRequest());

        JsonNode depois = json.readTree(mvc.perform(auth(post("/api/encomendas/" + encId + "/pagamentos"))
                        .content("{\"valor\":400}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(depois.get("status").asText()).isEqualTo("EM_PRODUCAO");
        assertThat(depois.get("saldo").decimalValue()).isEqualByComparingTo("800");

        // não aceita pagamento acima do saldo
        mvc.perform(auth(post("/api/encomendas/" + encId + "/pagamentos")).content("{\"valor\":900}"))
                .andExpect(status().isBadRequest());

        JsonNode fin = getJson("/api/financeiro");
        assertThat(fin.get("entradas").decimalValue()).isEqualByComparingTo("400");
        assertThat(fin.get("aReceber").decimalValue()).isEqualByComparingTo("800");

        JsonNode clientes = getJson("/api/clientes");
        assertThat(clientes).hasSize(1);
        assertThat(clientes.get(0).get("tipo").asText()).isEqualTo("NOIVA");

        JsonNode painel = getJson("/api/painel");
        assertThat(painel.get("emProducao").asLong()).isEqualTo(1);
        assertThat(painel.get("aReceber").decimalValue()).isEqualByComparingTo("800");
    }

    @Test
    void inscricaoNaOficinaComPixEListaDeEspera() throws Exception {
        JsonNode of = json.readTree(mvc.perform(auth(post("/api/oficinas")).content("""
                {"titulo":"Mandala para iniciantes","inicio":"%s","vagas":1,"valor":150}
                """.formatted(LocalDateTime.now().plusDays(5).withNano(0))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long ofId = of.get("id").asLong();

        mvc.perform(auth(put("/api/configuracoes")).content("""
                {"nomeAtelie":"Lenne Carvalho Ateliê","whatsapp":"98988793891","chavePix":"lenne@pix.com"}
                """)).andExpect(status().isOk());

        JsonNode site = json.readTree(mvc.perform(get("/api/publico/site")).andReturn().getResponse().getContentAsString());
        assertThat(site.get("oficinas")).hasSize(1);
        assertThat(site.get("atelie").get("whatsapp").asText()).isEqualTo("5598988793891");

        JsonNode insc = json.readTree(mvc.perform(post("/api/publico/oficinas/" + ofId + "/inscricoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carla\",\"whatsapp\":\"98 98222-3344\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(insc.get("status").asText()).isEqualTo("AGUARDANDO_PIX");
        assertThat(insc.get("chavePix").asText()).isEqualTo("lenne@pix.com");

        // mesmo WhatsApp não se inscreve duas vezes
        mvc.perform(post("/api/publico/oficinas/" + ofId + "/inscricoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Carla\",\"whatsapp\":\"98982223344\"}")).andExpect(status().isBadRequest());

        // oficina lotada: segunda pessoa vai para a lista de espera
        JsonNode espera = json.readTree(mvc.perform(post("/api/publico/oficinas/" + ofId + "/inscricoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bia\",\"whatsapp\":\"98983334455\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(espera.get("status").asText()).isEqualTo("LISTA_ESPERA");

        mvc.perform(post("/api/publico/inscricoes/" + insc.get("codigo").asText() + "/comprovante-enviado"))
                .andExpect(status().isOk());
        JsonNode pix = getJson("/api/inscricoes/pix-a-conferir");
        assertThat(pix).hasSize(1);
        assertThat(pix.get(0).get("status").asText()).isEqualTo("A_CONFERIR");

        mvc.perform(auth(patch("/api/inscricoes/" + pix.get(0).get("id").asLong() + "/status"))
                .content("{\"status\":\"CONFIRMADA\"}")).andExpect(status().isOk());

        JsonNode fin = getJson("/api/financeiro");
        assertThat(fin.get("entradas").decimalValue()).isEqualByComparingTo("150");
        JsonNode oficina = getJson("/api/oficinas/" + ofId);
        assertThat(oficina.get("confirmadas").asLong()).isEqualTo(1);
        assertThat(oficina.get("listaEspera").asLong()).isEqualTo(1);

        // desfazer a confirmação remove a entrada do financeiro
        mvc.perform(auth(patch("/api/inscricoes/" + pix.get(0).get("id").asLong() + "/status"))
                .content("{\"status\":\"CANCELADA\"}")).andExpect(status().isOk());
        assertThat(getJson("/api/financeiro").get("entradas").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void compraDeMaterialViraDespesaEAlertaDeEstoque() throws Exception {
        JsonNode m = json.readTree(mvc.perform(auth(post("/api/materiais")).content("""
                {"nome":"Cordão cru 5 mm","unidade":"rolo","quantidade":3,"estoqueMinimo":2,"custoUnitario":45}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = m.get("id").asLong();
        assertThat(m.get("abaixoDoMinimo").asBoolean()).isFalse();

        mvc.perform(auth(post("/api/materiais/" + id + "/ajuste")).content("{\"tipo\":\"SAIDA\",\"quantidade\":1}"))
                .andExpect(status().isOk());
        assertThat(getJson("/api/painel").get("materiaisBaixos")).hasSize(1);

        mvc.perform(auth(post("/api/materiais/" + id + "/ajuste")).content("{\"tipo\":\"SAIDA\",\"quantidade\":5}"))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(post("/api/materiais/" + id + "/ajuste"))
                .content("{\"tipo\":\"ENTRADA\",\"quantidade\":4,\"registrarDespesa\":true}")).andExpect(status().isOk());
        JsonNode fin = getJson("/api/financeiro");
        assertThat(fin.get("saidas").decimalValue()).isEqualByComparingTo("180");
        assertThat(getJson("/api/painel").get("materiaisBaixos")).isEmpty();
    }

    @Test
    void fotoDoPortfolioApareceNoSite() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        var dados = new MockMultipartFile("dados", "", "application/json",
                "{\"titulo\":\"Painel mandala\",\"categoria\":\"DECORACAO\"}".getBytes());
        var foto = new MockMultipartFile("foto", "mandala.png", "image/png", png);
        mvc.perform(multipart("/api/portfolio").file(dados).file(foto).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        var falsa = new MockMultipartFile("foto", "x.png", "image/png", "<script>".getBytes());
        mvc.perform(multipart("/api/portfolio").file(dados).file(falsa).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        JsonNode site = json.readTree(mvc.perform(get("/api/publico/site")).andReturn().getResponse().getContentAsString());
        assertThat(site.get("portfolio")).hasSize(1);
        String url = site.get("portfolio").get(0).get("url").asText();
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    private JsonNode getJson(String url) throws Exception {
        return json.readTree(mvc.perform(auth(get(url))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
