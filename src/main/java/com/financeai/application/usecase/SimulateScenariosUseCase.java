package com.financeai.application.usecase;

import com.financeai.domain.model.*;
import com.financeai.infrastructure.simulation.DefaultCalculationEngine;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SimulateScenariosUseCase {

    private final DefaultCalculationEngine engine;

    public SimulateScenariosUseCase(DefaultCalculationEngine engine) {
        this.engine = engine;
    }

    public List<ScenarioResult> execute(String userId, List<Transaction> transactions, FinancialGoal goal) {
        // 1. Diagnóstico base vindo do motor de cálculo
        FinancialDiagnostic baseDiagnostic = engine.calculate(userId, transactions, goal);

        // 2. Cálculo das Sobras Mensais (Surplus) para cada cenário
        BigDecimal currentSurplus = calculateMonthlySurplus(transactions, BigDecimal.ZERO);
        
        // Cenário RECOMENDADO (Green) - Melhor caso com otimização da mochila
        BigDecimal potentialSavings = calculatePotentialSavings(transactions);
        BigDecimal recommendedSurplus = calculateMonthlySurplus(transactions, potentialSavings);
        BigDecimal recommendedBalance = baseDiagnostic.projectedBalance30Days().add(potentialSavings);

        // Cenário ATUAL (Yellow) - Representa a inércia/comportamento real hoje
        BigDecimal currentBalance = baseDiagnostic.projectedBalance30Days();

        // Cenário PESSIMISTA (Red) - O pior cenário (Inércia + 15% de degradação)
        // Definido como o pior caso conforme sua solicitação
        BigDecimal pessimisticMultiplier = new BigDecimal("0.85"); 
        BigDecimal pessimisticSurplus = currentSurplus.multiply(pessimisticMultiplier);
        BigDecimal pessimisticBalance = currentBalance.multiply(pessimisticMultiplier);

        return List.of(
            new ScenarioResult(
                pessimisticBalance, 
                calculateMonths(baseDiagnostic.currentBalance(), goal, pessimisticSurplus), 
                "RED"
            ),
            new ScenarioResult(
                currentBalance, 
                calculateMonths(baseDiagnostic.currentBalance(), goal, currentSurplus), 
                "YELLOW"
            ),
            new ScenarioResult(
                recommendedBalance, 
                calculateMonths(baseDiagnostic.currentBalance(), goal, recommendedSurplus), 
                "GREEN"
            )
        );
    }

    private BigDecimal calculateMonthlySurplus(List<Transaction> transactions, BigDecimal savings) {
        BigDecimal income = transactions.stream()
            .filter(t -> t.amount().compareTo(BigDecimal.ZERO) > 0)
            .map(Transaction::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = transactions.stream()
            .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0)
            .map(t -> t.amount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sobra = Renda - (Gastos Brutos - Economia sugerida)
        return income.subtract(expenses.subtract(savings));
    }

    private BigDecimal calculatePotentialSavings(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0 && !t.isEssential())
            .map(t -> t.amount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateMonths(BigDecimal currentBalance, FinancialGoal goal, BigDecimal monthlySurplus) {
        // Se a sobra mensal for negativa ou zero, a meta é considerada inviável (99 meses)
        if (monthlySurplus.compareTo(BigDecimal.ZERO) <= 0) return 99;
        
        BigDecimal gap = goal.targetAmount().subtract(currentBalance);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) return 0;

        return gap.divide(monthlySurplus, 0, RoundingMode.CEILING).intValue();
    }
}