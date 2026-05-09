package com.financeai.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o fluxo completo: register → login → profile → transaction → report
 * Usa H2 in-memory com ddl-auto=create-drop (sem necessidade de PostgreSQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthAndUserFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Token compartilhado entre os testes (fluxo encadeado)
    private static String jwtToken;
    private static String transactionId;

    // ── Registro ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /auth/register → 201 com token JWT")
    void register_shouldReturn201WithToken() throws Exception {
        String body = """
                {"name":"Teste User","email":"test.flow@financeai.com","password":"senha123"}
                """;

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test.flow@financeai.com"))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        jwtToken = node.get("token").asText();
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/register → 400 ao duplicar email")
    void register_duplicateEmail_shouldReturn400() throws Exception {
        String body = """
                {"name":"Outro","email":"test.flow@financeai.com","password":"senha123"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/register → 400 com senha curta (< 6 chars)")
    void register_shortPassword_shouldReturn400() throws Exception {
        String body = """
                {"name":"Curto","email":"curto@financeai.com","password":"abc"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /auth/login → 200 com credenciais válidas")
    void login_validCredentials_shouldReturn200() throws Exception {
        String body = """
                {"email":"test.flow@financeai.com","password":"senha123"}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("POST /auth/login → 401 com senha incorreta")
    void login_wrongPassword_shouldReturn401() throws Exception {
        String body = """
                {"email":"test.flow@financeai.com","password":"errada999"}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ── Proteção de rotas ─────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /api/user → 401 sem token")
    void getUser_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/user → 200 com token válido")
    void getUser_withToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/user")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test.flow@financeai.com"));
    }

    // ── Usuário: campos imutáveis ─────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("PUT /api/user → 400 ao tentar alterar email")
    void updateUser_immutableEmail_shouldReturn400() throws Exception {
        String body = """
                {"email":"novo@email.com"}
                """;

        mockMvc.perform(put("/api/user")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("E-mail não pode ser alterado."));
    }

    @Test
    @Order(9)
    @DisplayName("PUT /api/user → 400 ao tentar alterar senha")
    void updateUser_immutablePassword_shouldReturn400() throws Exception {
        String body = """
                {"password":"nova123"}
                """;

        mockMvc.perform(put("/api/user")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Senha não pode ser alterada."));
    }

    @Test
    @Order(10)
    @DisplayName("PUT /api/user → 200 ao atualizar systemRole para PREMIUM")
    void updateUser_systemRole_shouldReturn200() throws Exception {
        String body = """
                {"systemRole":"PREMIUM"}
                """;

        mockMvc.perform(put("/api/user")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemRole").value("PREMIUM"));
    }

    // ── Perfil ────────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("GET /api/profile → 200 e cria perfil padrão se não existir")
    void getProfile_shouldReturn200WithDefaults() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportPreference").value("MONTHLY"))
                .andExpect(jsonPath("$.data.hasOpenFinanceActive").value(false));
    }

    @Test
    @Order(12)
    @DisplayName("PUT /api/profile → 200 ao atualizar reportPreference para WEEKLY")
    void updateProfile_reportPreference_shouldReturn200() throws Exception {
        String body = """
                {"reportPreference":"WEEKLY"}
                """;

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportPreference").value("WEEKLY"));
    }

    @Test
    @Order(13)
    @DisplayName("PUT /api/profile → 400 com reportPreference inválido")
    void updateProfile_invalidInterval_shouldReturn400() throws Exception {
        String body = """
                {"reportPreference":"DIARIO"}
                """;

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    @DisplayName("PUT /api/profile → 400 ao tentar alterar holderType (campo imutável)")
    void updateProfile_immutableHolderType_shouldReturn400() throws Exception {
        String body = """
                {"holderType":"PJ"}
                """;

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Transações ────────────────────────────────────────────────────────────

    @Test
    @Order(15)
    @DisplayName("GET /api/transaction → 200 com lista vazia inicialmente")
    void listTransactions_empty_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/transaction")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @DisplayName("POST /api/transaction/generate → 400 com count=0")
    void generateTransactions_countZero_shouldReturn400() throws Exception {
        String body = """
                {"count":0}
                """;

        mockMvc.perform(post("/api/transaction/generate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(17)
    @DisplayName("POST /api/transaction/generate → 400 com count=11 (acima do max)")
    void generateTransactions_countAboveMax_shouldReturn400() throws Exception {
        String body = """
                {"count":11}
                """;

        mockMvc.perform(post("/api/transaction/generate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(18)
    @DisplayName("POST /api/transaction/generate → 201 com count=3")
    void generateTransactions_shouldReturn201() throws Exception {
        String body = """
                {"count":3}
                """;

        MvcResult result = mockMvc.perform(post("/api/transaction/generate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        transactionId = node.get("data").get(0).get("id").asText();
    }

    @Test
    @Order(19)
    @DisplayName("POST /api/transaction/generate?fromOtherBank=true → 400 sem open finance ativo")
    void generateTransactions_openFinanceDisabled_shouldReturn400() throws Exception {
        String body = """
                {"count":2}
                """;

        mockMvc.perform(post("/api/transaction/generate?fromOtherBank=true")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(20)
    @DisplayName("DELETE /api/transaction/{id} → 200 com id válido")
    void deleteTransaction_validId_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/transaction/" + transactionId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transação excluída com sucesso."));
    }

    @Test
    @Order(21)
    @DisplayName("DELETE /api/transaction/{id} → 400 ao deletar transação já deletada ou inexistente")
    void deleteTransaction_notFound_shouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/transaction/id-inexistente-xyz")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    // ── Relatórios ────────────────────────────────────────────────────────────

    @Test
    @Order(22)
    @DisplayName("GET /api/report/list → 200 com lista vazia antes de gerar")
    void listReports_empty_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/report/list")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("GET /api/report/{id} → 200 com null data para id inexistente")
    void getReport_notFound_shouldReturn200WithNullData() throws Exception {
        mockMvc.perform(get("/api/report/id-que-nao-existe")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ── Análise ───────────────────────────────────────────────────────────────

    @Test
    @Order(24)
    @DisplayName("POST /api/analysis/report → 200 gera e persiste relatório")
    void generateReport_shouldReturn200() throws Exception {
        String body = """
                {"interval":"MONTHLY"}
                """;

        mockMvc.perform(post("/api/analysis/report")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.header").exists())
                .andExpect(jsonPath("$.data.report.reportBalance").exists());
    }
}
