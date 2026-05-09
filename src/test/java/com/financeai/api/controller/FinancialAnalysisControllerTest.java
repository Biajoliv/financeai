package com.financeai.api.controller;

import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; 

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "perfil.clt.senior@email.com")
class FinancialAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve retornar 400 ao enviar letras em campos numericos")
    void shouldReturn400WhenInvalidDataType() throws Exception {
        String invalidJson = """
            {
                "transactions": [
                    {
                        "description": "Erro",
                        "amount": "ABC",
                        "date": "2026-05-01",
                        "category": "OTHER",
                        "isEssential": true,
                        "priority": 5
                    }
                ],
                "goal": {
                    "name": "Meta",
                    "targetAmount": 1000.00,
                    "deadline": "2026-12-01",
                    "profile": "INDIVIDUAL"
                }
            }
            """;

        mockMvc.perform(post("/api/analysis/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve processar simulacao com sucesso")
    void shouldProcessSimulationSuccessfully() throws Exception {
        String validJson = """
            {
                "transactions": [
                    {
                        "description": "Salario",
                        "amount": 5000.00,
                        "date": "2026-05-01",
                        "category": "SALARY",
                        "isEssential": true,
                        "priority": 5
                    }
                ],
                "goal": {
                    "name": "Reserva",
                    "targetAmount": 10000.00,
                    "deadline": "2026-12-31",
                    "profile": "INDIVIDUAL"
                }
            }
            """;

        mockMvc.perform(post("/api/analysis/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isOk());
    }
}