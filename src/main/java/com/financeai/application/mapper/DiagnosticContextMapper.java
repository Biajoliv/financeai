package com.financeai.application.mapper;

import com.financeai.domain.model.FinancialDiagnostic;
import com.financeai.domain.model.FinancialGoal;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DiagnosticContextMapper {

    /**
     * Transforma o diagnóstico técnico em um "chunk" de texto para o Vector Store/RAG.
     */
    public String toAIContext(FinancialDiagnostic diagnostic, FinancialGoal goal) {
        String alerts = String.join("; ", diagnostic.alerts());
        
        return String.format(
            "CONTEXTO FINANCEIRO DO USUÁRIO:\\n" +
            "- Objetivo: %s com meta de R$ %.2f.\\n" +
            "- Saldo Atual Ajustado: R$ %.2f.\\n" +
            "- Projeção para 30 dias: R$ %.2f.\\n" +
            "- Dias até ficar negativo: %s.\\n" +
            "- Alertas Identificados: [%s].\\n" +
            "- Sugestões de Corte: %s.",
            goal.name(), goal.targetAmount(),
            diagnostic.currentBalance(),
            diagnostic.projectedBalance30Days(),
            diagnostic.daysUntilNegativeBalance() == null ? "Sem risco" : diagnostic.daysUntilNegativeBalance(),
            alerts,
            diagnostic.recommendationChunks().stream().collect(Collectors.joining(", "))
        );
    }
}