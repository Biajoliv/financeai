package com.financeai.infrastructure.simulation;

import com.financeai.domain.model.*;
import com.financeai.domain.service.CalculationEngine;
import com.financeai.infrastructure.utils.DataSanitizer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DefaultCalculationEngine implements CalculationEngine {

    private final DataSanitizer sanitizer;
    private final Map<String, Double> categoryWeights;

    public DefaultCalculationEngine(DataSanitizer sanitizer) {
        this.sanitizer = sanitizer;
        Map<String, Double> weights = new HashMap<>();
        weights.put("EDUCATION", 2.0);
        weights.put("HEALTH", 2.5);
        weights.put("ENTERTAINMENT", 0.5);
        weights.put("SUBSCRIPTIONS", 0.7);
        this.categoryWeights = Collections.unmodifiableMap(weights);
    }

    @Override
    public FinancialDiagnostic calculate(String userId, List<Transaction> transactions, FinancialGoal goal) {
        if (transactions == null || transactions.isEmpty()) {
            return createEmptyDiagnostic();
        }

        // Java 21: Record Pattern Matching para log e rastreabilidade por Tenant [cite: 282, 312]
        if (goal instanceof FinancialGoal(String name, BigDecimal target, LocalDate deadline, UserProfile profile)) {
            System.out.println("LOG [Tenant: " + userId + "] - Analisando meta: " + name);
        }

        // 1. Sanitização e Limpeza de Dados
        List<Transaction> cleanTransactions = transactions.stream()
                .map(t -> new Transaction(
                        sanitizer.sanitizeDescription(t.description()),
                        t.amount(), t.date(), t.category(), t.isEssential(), t.priority()))
                .collect(Collectors.toList());

        BigDecimal currentBalance = calculateInitialBalance(cleanTransactions);
        
        // 2. Aplicação da Estratégia Fiscal (Uso de Sealed Interface) [cite: 169, 172, 360]
        StrategyCalculation strategy = selectStrategy(goal);
        BigDecimal adjustedBalance = strategy.applyTaxDeduction(currentBalance);

        // 3. Cálculos de Taxas e Liquidez
        BigDecimal dailyRate = calculateAverageDailySpending(cleanTransactions);
        List<String> alerts = new ArrayList<>();

        Integer dayOfRisk = simulateLiquidity(adjustedBalance, dailyRate, alerts);
        BigDecimal monthlyGap = calculateMonthlyGap(adjustedBalance, goal);
        List<String> recommendations = new ArrayList<>();

        // 4. Otimização (Variação do Problema da Mochila) [cite: 218, 219, 358]
        List<Transaction> suggestedCuts = optimizeSavings(cleanTransactions, monthlyGap);
        BigDecimal totalPotentialSaving = suggestedCuts.stream()
                .map(t -> t.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!suggestedCuts.isEmpty()) {
            recommendations.add("OTIMIZACAO_CORTES_DISPONIVEL");
            alerts.add(String.format("Sugestão de economia: R$ %.2f focando em itens não essenciais.", 
                    totalPotentialSaving));
        }

        // 5. Projeção de Prazo e Formatação
        if (totalPotentialSaving.compareTo(monthlyGap) < 0 || monthlyGap.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate suggestedDate = calculateNewDeadline(adjustedBalance, goal, totalPotentialSaving);
            
            Locale ptBr = Locale.of("pt", "BR");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBr);
            String formattedDate = suggestedDate.format(formatter);
            formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);

            alerts.add(String.format("ALERTA ESTRATÉGICO: Sugerimos estender o prazo para %s.", formattedDate));
            recommendations.add("REVISAO_ESTRUTURAL_NECESSARIA");
        }

        return new FinancialDiagnostic(
                adjustedBalance,
                adjustedBalance.subtract(dailyRate.multiply(new BigDecimal("30"))),
                dayOfRisk,
                alerts,
                recommendations);
    }

    // --- Métodos Auxiliares ---

    private Integer simulateLiquidity(BigDecimal balance, BigDecimal dailyRate, List<String> alerts) {
        if (dailyRate.compareTo(BigDecimal.ZERO) <= 0) return null;
        
        BigDecimal simulated = balance;
        for (int day = 1; day <= 30; day++) {
            simulated = simulated.subtract(dailyRate);
            if (simulated.compareTo(BigDecimal.ZERO) <= 0) {
                alerts.add("Risco de liquidez detectado no curto prazo.");
                return day;
            }
        }
        return null;
    }

    private List<Transaction> optimizeSavings(List<Transaction> transactions, BigDecimal gap) {
        if (gap.compareTo(BigDecimal.ZERO) <= 0) return Collections.emptyList();
        
        return transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0 && !t.isEssential())
                .sorted((t1, t2) -> {
                    double weight1 = categoryWeights.getOrDefault(t1.category(), 1.0);
                    double weight2 = categoryWeights.getOrDefault(t2.category(), 1.0);
                    double eff1 = (t1.amount().abs().doubleValue() / Math.pow(t1.priority(), 2)) / weight1;
                    double eff2 = (t2.amount().abs().doubleValue() / Math.pow(t2.priority(), 2)) / weight2;
                    return Double.compare(eff2, eff1);
                })
                .collect(Collectors.toList());
    }

    private LocalDate calculateNewDeadline(BigDecimal balance, FinancialGoal goal, BigDecimal monthlySurplus) {
        if (monthlySurplus.compareTo(BigDecimal.ZERO) <= 0) return goal.deadline().plusYears(1);
        
        BigDecimal remaining = goal.targetAmount().subtract(balance);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return LocalDate.now();
        
        long monthsNeeded = remaining.divide(monthlySurplus, 0, RoundingMode.CEILING).longValue();
        LocalDate projected = LocalDate.now().plusMonths(monthsNeeded);
        
        return (projected.getDayOfMonth() > 15) 
                ? projected.plusMonths(1).withDayOfMonth(1) 
                : projected.withDayOfMonth(1);
    }

    private StrategyCalculation selectStrategy(FinancialGoal goal) {
        return (goal.profile() == UserProfile.BUSINESS) 
                ? new BusinessStrategy(new BigDecimal("75.00"), BigDecimal.ZERO) 
                : new IndividualStrategy();
    }

    private BigDecimal calculateInitialBalance(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageDailySpending(List<Transaction> transactions) {
        BigDecimal essentialOut = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0 && t.isEssential())
                .map(t -> t.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return essentialOut.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMonthlyGap(BigDecimal balance, FinancialGoal goal) {
        BigDecimal remaining = goal.targetAmount().subtract(balance);
        return (remaining.compareTo(BigDecimal.ZERO) > 0) 
                ? remaining.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
    }

    private FinancialDiagnostic createEmptyDiagnostic() {
        return new FinancialDiagnostic(BigDecimal.ZERO, BigDecimal.ZERO, null,
                List.of("Dados insuficientes."), List.of("WAITING_FOR_DATA"));
    }
}