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
                  "diagnostico": "O usuário apresenta desequilíbrio financeiro mensal e precisa reorganizar gastos para atingir o objetivo.",
                  "nivel_risco": "alto",
                  "principais_problemas": [
                    "Gastos variáveis elevados",
                    "Baixa previsibilidade financeira"
                  ],
                  "recomendacoes": [
                    "Reduzir gastos não essenciais",
                    "Definir um limite semanal de despesas",
                    "Separar uma quantia fixa no início do mês"
                  ],
                  "proximos_passos": [
                    "Revisar as últimas transações",
                    "Definir uma meta mensal realista"
                  ],
                  "alertas": [
                    "A análise é baseada nos dados enviados e pode mudar com novas informações"
                  ],
                  "confianca": 0.86
                }
                """;
    }
}