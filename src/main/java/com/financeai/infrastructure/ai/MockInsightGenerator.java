package com.financeai.infrastructure.ai;

import com.financeai.domain.port.InsightGenerator;
import org.springframework.stereotype.Service;

@Service
public class MockInsightGenerator implements InsightGenerator {

    @Override
    public String gerar(String prompt) {

        return """
        {
          "diagnostico": "Seu objetivo é viável, mas exige redução de gastos impulsivos.",
          "recomendacoes": [
            "Reduzir gastos com delivery",
            "Separar dinheiro no início do mês",
            "Cancelar assinaturas pouco usadas"
          ],
          "confianca": 0.81
        }
        """;
    }
}