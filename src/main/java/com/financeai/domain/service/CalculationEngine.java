package com.financeai.domain.service;

import com.financeai.domain.model.FinancialDiagnostic;
import com.financeai.domain.model.FinancialGoal;
import com.financeai.domain.model.Transaction;
import java.util.List;

public interface CalculationEngine {
    /**
     * Realiza os cálculos determinísticos baseados no histórico e objetivo.
     */
    FinancialDiagnostic calculate(String userId, List<Transaction> transactions, FinancialGoal goal);
}