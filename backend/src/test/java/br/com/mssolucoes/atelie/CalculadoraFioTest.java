package br.com.mssolucoes.atelie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 6: dados do cordão e amostras de nó para a calculadora de fio. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CalculadoraFioTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;

    @BeforeEach
    void preparar() throws Exception {
        token = entrar("teste@atelie.local", "senha-teste-123");
    }

    @Test
    void materialGuardaDadosDoCordaoEAmostrasDeNoComFator() throws Exception {
        JsonNode m = enviar(post("/api/materiais"), """
                {"nome":"Cordão cru 5 mm","unidade":"rolo","quantidade":3,"estoqueMinimo":1,"custoUnitario":45,
                 "fiosPorCm":2,"gramasPorMetro":22.5,"metrosPorUnidade":100}""", status().isCreated());
        long id = m.get("id").asLong();
        assertThat(m.get("fiosPorCm").decimalValue()).isEqualByComparingTo("2");
        assertThat(m.get("metrosPorUnidade").decimalValue()).isEqualByComparingTo("100");

        // Sem os dados opcionais continua funcionando, e limpar volta a nulo
        JsonNode simples = enviar(post("/api/materiais"),
                "{\"nome\":\"Argola\",\"unidade\":\"unidade\",\"quantidade\":0,\"estoqueMinimo\":0,\"custoUnitario\":2}", status().isCreated());
        assertThat(simples.get("fiosPorCm").isNull()).isTrue();
        enviar(post("/api/materiais"), "{\"nome\":\"X\",\"unidade\":\"metro\",\"quantidade\":0,\"estoqueMinimo\":0,\"custoUnitario\":0,\"fiosPorCm\":0}",
                status().isBadRequest());

        // Amostra: 100 cm de fio renderam 20 cm em nó quadrado → fator 5
        JsonNode a = enviar(post("/api/materiais/" + id + "/amostras"),
                "{\"nomeNo\":\" Nó quadrado \",\"fioCm\":100,\"rendeuCm\":20,\"observacao\":\"bem apertado\"}", status().isCreated());
        assertThat(a.get("fator").decimalValue()).isEqualByComparingTo("5");
        assertThat(a.get("nomeNo").asText()).isEqualTo("Nó quadrado");
        enviar(post("/api/materiais/" + id + "/amostras"), "{\"nomeNo\":\"Espiral\",\"fioCm\":90,\"rendeuCm\":30}", status().isCreated());

        // Rendeu mais que o fio usado: medida errada
        enviar(post("/api/materiais/" + id + "/amostras"), "{\"nomeNo\":\"Espiral\",\"fioCm\":10,\"rendeuCm\":30}", status().isBadRequest());
        enviar(post("/api/materiais/" + id + "/amostras"), "{\"nomeNo\":\"\",\"fioCm\":10,\"rendeuCm\":5}", status().isBadRequest());

        JsonNode lista = enviar(get("/api/materiais/" + id + "/amostras"), null, status().isOk());
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).get("nomeNo").asText()).isEqualTo("Espiral");
        assertThat(lista.get(0).get("fator").decimalValue()).isEqualByComparingTo("3");

        // A lista de materiais mostra quantas amostras cada um tem
        for (JsonNode x : enviar(get("/api/materiais"), null, status().isOk())) {
            if (x.get("id").asLong() == id) assertThat(x.get("amostras").asInt()).isEqualTo(2);
        }

        enviar(delete("/api/materiais/amostras/" + a.get("id").asLong()), null, status().isNoContent());
        assertThat(enviar(get("/api/materiais/" + id + "/amostras"), null, status().isOk())).hasSize(1);
        enviar(get("/api/materiais/999/amostras"), null, status().isNotFound());

        // Material sem uso no histórico pode ser excluído mesmo com amostras (elas vão junto)
        enviar(delete("/api/materiais/" + id), null, status().isNoContent());
    }

    @Test
    void ajudanteNaoMexeNasAmostras() throws Exception {
        long id = enviar(post("/api/materiais"),
                "{\"nome\":\"Cordão\",\"unidade\":\"metro\",\"quantidade\":10,\"estoqueMinimo\":0,\"custoUnitario\":0.5}", status().isCreated())
                .get("id").asLong();
        enviar(post("/api/equipe"), "{\"nome\":\"Rita\",\"email\":\"rita@atelie.local\",\"perfil\":\"AJUDANTE\",\"senha\":\"rita-senha-1\"}",
                status().isCreated());
        token = entrar("rita@atelie.local", "rita-senha-1");
        enviar(get("/api/materiais/" + id + "/amostras"), null, status().isForbidden());
        enviar(post("/api/materiais/" + id + "/amostras"), "{\"nomeNo\":\"Nó\",\"fioCm\":100,\"rendeuCm\":20}", status().isForbidden());
    }

    private String entrar(String email, String senha) throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    private JsonNode enviar(MockHttpServletRequestBuilder b, String corpo, ResultMatcher esperado) throws Exception {
        MockHttpServletRequestBuilder req = b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
        if (corpo != null) req = req.content(corpo);
        String r = mvc.perform(req).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }
}
