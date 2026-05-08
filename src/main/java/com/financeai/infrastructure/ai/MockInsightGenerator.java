package com.financeai.infrastructure.ai;

import com.financeai.domain.port.InsightGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!openai")
public class MockInsightGenerator implements InsightGenerator {

    @Override
    public String gerar(String prompt) {

        return """
                {
                  "diagnostico": "Seu objetivo é viável, mas exige redução de gastos e maior organização financeira.",
                  "recomendacoes": [
                    "Reduzir gastos com delivery",
                    "Separar dinheiro no início do mês",
                    "Cancelar assinaturas pouco usadas"
                  ],
                  "riscos": [
                    "Gastos impulsivos",
                    "Falta de reserva de emergência"
                  ],
                  "proximos_passos": [
                    "Criar orçamento mensal",
                    "Definir meta de economia semanal"
                  ],
                  "confianca": 0.81
                }
                """;
    }
}