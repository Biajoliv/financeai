package com.financeai.application.usecase;

import com.financeai.domain.model.FinancialDiagnostic;
import com.financeai.domain.model.FinancialGoal;
import com.financeai.domain.model.Transaction;
import com.financeai.domain.service.CalculationEngine;
import com.financeai.infrastructure.simulation.MockIngestionService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnalyzeGoalUseCase {

    private final MockIngestionService ingestionService;
    private final CalculationEngine calculationEngine;

    public AnalyzeGoalUseCase(MockIngestionService ingestionService, CalculationEngine calculationEngine) {
        this.ingestionService = ingestionService;
        this.calculationEngine = calculationEngine;
    }

    public FinancialDiagnostic execute(String userId, FinancialGoal goal) {
        List<Transaction> transactions = ingestionService.loadTransactions();
        return calculationEngine.calculate(userId, transactions, goal);
    }
}