package com.financeai.application.usecase;

import com.financeai.domain.model.FinancialDiagnostic;
import com.financeai.domain.model.FinancialGoal;
import com.financeai.domain.service.CalculationEngine;
import com.financeai.domain.service.TransactionProvider;
import org.springframework.stereotype.Service;

/**
 * PRINCÍPIO SOLID APLICADO: Dependency Inversion Principle (DIP)
 *
 * PROBLEMA IDENTIFICADO:
 * Esta classe dependia diretamente de MockIngestionService (classe concreta
 * de infraestrutura). Use cases de aplicação são camada de alto nível —
 * devem depender de abstrações, não de detalhes de implementação.
 *
 *   // ANTES (violação DIP):
 *   private final MockIngestionService ingestionService;
 *
 * SOLUÇÃO:
 * Substituído por TransactionProvider (interface de domínio). O Spring injeta
 * a implementação concreta (MockTransactionProvider por enquanto, futuramente
 * DatabaseTransactionProvider) sem que este use case saiba a diferença.
 *
 * REGRA DE DEPENDÊNCIA (Clean Architecture):
 * Use cases → dependem de → interfaces de domínio ← implementadas por → infraestrutura
 * A seta de dependência aponta para dentro, nunca para fora.
 */
@Service
public class AnalyzeGoalUseCase {

    private final TransactionProvider transactionProvider;
    private final CalculationEngine calculationEngine;

    public AnalyzeGoalUseCase(TransactionProvider transactionProvider, CalculationEngine calculationEngine) {
        this.transactionProvider = transactionProvider;
        this.calculationEngine = calculationEngine;
    }

    public FinancialDiagnostic execute(String userId, FinancialGoal goal) {
        var transactions = transactionProvider.getTransactionsByUserId(userId);
        return calculationEngine.calculate(userId, transactions, goal);
    }
}
