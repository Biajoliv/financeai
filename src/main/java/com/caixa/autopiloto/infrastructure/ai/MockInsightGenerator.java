package com.caixa.autopiloto.infrastructure.ai;

import com.caixa.autopiloto.domain.port.InsightGenerator;
import org.springframework.stereotype.Component;

@Component
public class MockInsightGenerator implements InsightGenerator {

    @Override
    public String gerar(String prompt) {
        return """
                {
                  "diagnostico": "objetivo inviável no ritmo atual",
                  "resumo": "Com base nos dados informados, a sobra mensal ainda não é suficiente para atingir o objetivo no prazo desejado.",
                  "recomendacoes": [
                    "Reduzir gastos variáveis, começando por delivery e lazer.",
                    "Cancelar assinaturas não utilizadas.",
                    "Separar automaticamente uma parte da renda no início do mês."
                  ],
                  "pii_removida": true,
                  "confianca": 0.81,
                  "chunks_usados": ["educ_fin_001", "educ_fin_003", "caixa_prod_001"]
                }
                """;
    }
}