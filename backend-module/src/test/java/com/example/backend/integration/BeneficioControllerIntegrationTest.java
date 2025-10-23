package com.example.backend.integration;

import com.example.backend.dto.request.BeneficioCreateRequest;
import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.dto.response.BeneficioResponse;
import com.example.backend.repository.BeneficioRepository;
import com.example.backend.repository.TransferenciaLogRepository;
import com.example.ejb.Beneficio;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeneficioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BeneficioRepository beneficioRepository;

    @Autowired
    private TransferenciaLogRepository logRepository;

    @Autowired
    private EntityManager entityManager;

    private Beneficio beneficio1;
    private Beneficio beneficio2;

    @BeforeEach
    void setUp() {
        logRepository.deleteAll();
        beneficioRepository.deleteAll();
        beneficioRepository.flush();
        entityManager.clear();

        beneficio1 = new Beneficio("Benefício Teste 1", "Descrição 1", new BigDecimal("1000.00"));
        beneficio2 = new Beneficio("Benefício Teste 2", "Descrição 2", new BigDecimal("500.00"));

        beneficio1 = beneficioRepository.saveAndFlush(beneficio1);
        beneficio2 = beneficioRepository.saveAndFlush(beneficio2);
    }

    @Test
    @DisplayName("GET /api/v1/beneficios - 200 OK com lista de benefícios")
    void deveRetornar200ComListaDeBeneficios() throws Exception {

        mockMvc.perform(get("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(beneficio1.getId()))
                .andExpect(jsonPath("$[0].nome").value("Benefício Teste 1"))
                .andExpect(jsonPath("$[0].valor").value(1000.00))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[1].id").value(beneficio2.getId()))
                .andExpect(jsonPath("$[1].nome").value("Benefício Teste 2"));
    }

    @Test
    @DisplayName("GET /api/v1/beneficios - 200 OK com lista vazia")
    void deveRetornar200ComListaVazia() throws Exception {
        beneficioRepository.deleteAll();

        mockMvc.perform(get("/api/v1/beneficios"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/beneficios/{id} - 200 OK com benefício encontrado")
    void deveRetornar200ComBeneficioEncontrado() throws Exception {
        mockMvc.perform(get("/api/v1/beneficios/{id}", beneficio1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(beneficio1.getId()))
                .andExpect(jsonPath("$.nome").value("Benefício Teste 1"))
                .andExpect(jsonPath("$.descricao").value("Descrição 1"))
                .andExpect(jsonPath("$.valor").value(1000.00))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/beneficios/{id} - 404 NOT FOUND se não existe")
    void deveRetornar404QuandoBeneficioNaoExiste() throws Exception {
        Long idInexistente = 999L;

        mockMvc.perform(get("/api/v1/beneficios/{id}", idInexistente))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios - 201 CREATED com benefício válido")
    void deveRetornar201AoCriarBeneficio() throws Exception {
        BeneficioCreateRequest request = new BeneficioCreateRequest();
        request.setNome("Novo Benefício");
        request.setDescricao("Descrição do novo benefício");
        request.setValor(new BigDecimal("750.50"));
        request.setAtivo(true);

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Novo Benefício"))
                .andExpect(jsonPath("$.descricao").value("Descrição do novo benefício"))
                .andExpect(jsonPath("$.valor").value(750.50))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        BeneficioResponse response = objectMapper.readValue(responseJson, BeneficioResponse.class);

        Beneficio salvo = beneficioRepository.findById(response.getId()).orElseThrow();
        assertThat(salvo.getNome()).isEqualTo("Novo Benefício");
        assertThat(salvo.getValor()).isEqualByComparingTo(new BigDecimal("750.50"));

    }

    @Test
    @DisplayName("POST /api/v1/beneficios - 400 BAD REQUEST com nome vazio")
    void deveRetornar400ComNomeVazio() throws Exception {
        BeneficioCreateRequest request = new BeneficioCreateRequest();
        request.setNome("");
        request.setDescricao("Descrição válida");
        request.setValor(new BigDecimal("100.00"));
        request.setAtivo(true);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios - 400 BAD REQUEST com valor negativo")
    void deveRetornar400ComValorNegativo() throws Exception {
        // Arrange
        BeneficioCreateRequest request = new BeneficioCreateRequest();
        request.setNome("Benefício Teste");
        request.setDescricao("Descrição");
        request.setValor(new BigDecimal("-100.00"));
        request.setAtivo(true);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios - 400 BAD REQUEST com corpo vazio")
    void deveRetornar400ComCorpoVazio() throws Exception {

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/beneficios/{id} - 200 OK com atualização válida")
    void deveRetornar200AoAtualizarBeneficio() throws Exception {
        BeneficioCreateRequest request = new BeneficioCreateRequest();
        request.setNome("Nome Atualizado");
        request.setDescricao("Descrição Atualizada");
        request.setValor(new BigDecimal("2000.00"));
        request.setAtivo(true);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/v1/beneficios/{id}", beneficio1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beneficio1.getId()))
                .andExpect(jsonPath("$.nome").value("Nome Atualizado"))
                .andExpect(jsonPath("$.descricao").value("Descrição Atualizada"))
                .andExpect(jsonPath("$.valor").value(2000.00));

        entityManager.clear();
        Beneficio atualizado = beneficioRepository.findById(beneficio1.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Nome Atualizado");
        assertThat(atualizado.getValor()).isEqualByComparingTo(new BigDecimal("2000.00"));

    }

    @Test
    @DisplayName("PUT /api/v1/beneficios/{id} - 404 NOT FOUND se não existe")
    void deveRetornar404AoAtualizarBeneficioInexistente() throws Exception {
        Long idInexistente = 999L;
        BeneficioCreateRequest request = new BeneficioCreateRequest();
        request.setNome("Teste");
        request.setDescricao("Teste");
        request.setValor(new BigDecimal("100.00"));
        request.setAtivo(true);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/v1/beneficios/{id}", idInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/beneficios/{id} - 400 BAD REQUEST com dados inválidos")
    void deveRetornar400AoAtualizarComDadosInvalidos() throws Exception {
        BeneficioCreateRequest request = new BeneficioCreateRequest();
        request.setNome("");
        request.setDescricao("Teste");
        request.setValor(new BigDecimal("100.00"));
        request.setAtivo(true);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/v1/beneficios/{id}", beneficio1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/beneficios/{id} - 204 NO CONTENT quando desativa")
    void deveRetornar204AoDesativarBeneficio() throws Exception {
        mockMvc.perform(delete("/api/v1/beneficios/{id}", beneficio1.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        entityManager.clear();
        Beneficio desativado = beneficioRepository.findById(beneficio1.getId()).orElseThrow();
        assertThat(desativado.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/v1/beneficios/{id} - 404 NOT FOUND se não existe")
    void deveRetornar404AoDesativarBeneficioInexistente() throws Exception {
        Long idInexistente = 999L;

        mockMvc.perform(delete("/api/v1/beneficios/{id}", idInexistente))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer - 200 OK com transferência válida")
    void deveRetornar200AoExecutarTransferencia() throws Exception {
        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1.getId());
        request.setToId(beneficio2.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk());

        entityManager.clear();
        Beneficio origem = beneficioRepository.findById(beneficio1.getId()).orElseThrow();
        Beneficio destino = beneficioRepository.findById(beneficio2.getId()).orElseThrow();

        assertThat(origem.getValor()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(destino.getValor()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer - 400 BAD REQUEST com dados inválidos")
    void deveRetornar400ComTransferenciaInvalida() throws Exception {
        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(null);
        request.setToId(beneficio2.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer - 422 com saldo insuficiente")
    void deveRetornar422ComSaldoInsuficiente() throws Exception {
        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1.getId());
        request.setToId(beneficio2.getId());
        request.setAmount(new BigDecimal("10000.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer - 404 se benefício não existe")
    void deveRetornar404ComBeneficioInexistente() throws Exception {
        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(999L);
        request.setToId(beneficio2.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer - Idempotência: mesma key não executa 2x")
    void deveRespeitarIdempotencia() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1.getId());
        request.setToId(beneficio2.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey(idempotencyKey);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        entityManager.clear();
        Beneficio origem1 = beneficioRepository.findById(beneficio1.getId()).orElseThrow();
        BigDecimal saldoApos1 = origem1.getValor();

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        entityManager.clear();
        Beneficio origem2 = beneficioRepository.findById(beneficio1.getId()).orElseThrow();
        BigDecimal saldoApos2 = origem2.getValor();

        assertThat(saldoApos2).isEqualByComparingTo(saldoApos1);
        assertThat(saldoApos2).isEqualByComparingTo(new BigDecimal("900.00"));
    }
}