package com.financeai.domain.port;

import com.financeai.domain.model.FinancialDiagnostic;
import com.financeai.domain.model.FinancialGoal;
import com.financeai.domain.model.Transaction;
import java.util.List;

public interface CalculationEngine {
    FinancialDiagnostic calculate(String userId, List<Transaction> transactions, FinancialGoal goal);
}
