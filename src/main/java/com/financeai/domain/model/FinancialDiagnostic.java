package com.financeai.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa o diagnóstico final gerado pelo motor de cálculo.
 */
public record FinancialDiagnostic(
    BigDecimal currentBalance,
    BigDecimal projectedBalance30Days,
    Integer daysUntilNegativeBalance, // "Dia D" de risco
    List<String> alerts,
    List<String> recommendationChunks // IDs para o RAG da IA
) {}