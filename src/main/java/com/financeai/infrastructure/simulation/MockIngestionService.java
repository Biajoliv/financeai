package com.financeai.infrastructure.simulation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financeai.domain.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockIngestionService {

    private final ObjectMapper objectMapper;

    public MockIngestionService() {
        // Configuramos o ObjectMapper para lidar com as datas do Java 8 (LocalDate)
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<Transaction> loadTransactions() {
        // Simulando o JSON bruto que seria recebido de uma integração
        String rawJson = """
            [
                {
                    "description": "SALARIO MENSAL 992837",
                    "amount": 5000.00,
                    "date": "2026-05-01",
                    "category": "INCOME",
                    "isEssential": true,
                    "priority": 5
                },
                {
                    "description": "ALUGUEL IMOVEL XPTO",
                    "amount": -2200.00,
                    "date": "2026-05-02",
                    "category": "HOUSING",
                    "isEssential": true,
                    "priority": 5
                },
                {
                    "description": "REST. COMIDA JAPONESA 4421",
                    "amount": -350.00,
                    "date": "2026-05-03",
                    "category": "FOOD",
                    "isEssential": false,
                    "priority": 2
                },
                {
                    "description": "ASSINATURA STREAMING XYZ",
                    "amount": -55.90,
                    "date": "2026-05-04",
                    "category": "ENTERTAINMENT",
                    "isEssential": false,
                    "priority": 1
                },
                {
                    "description": "COMPRA SUPERMERCADO 112233",
                    "amount": -800.00,
                    "date": "2026-05-05",
                    "category": "GROCERIES",
                    "isEssential": true,
                    "priority": 4
                }
            ]
            """;

        try {
            // Converte a String JSON diretamente para uma lista de Transactions
            return objectMapper.readValue(rawJson, new TypeReference<List<Transaction>>() {});
        } catch (Exception e) {
            // Em caso de erro na simulação do JSON, retorna lista vazia
            return List.of();
        }
    }
}