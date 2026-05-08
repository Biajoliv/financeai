package com.financeai.infrastructure.simulation;

import com.financeai.domain.model.*;
import com.financeai.domain.service.CalculationEngine;
import com.financeai.domain.service.WeightStrategy;
import com.financeai.infrastructure.utils.DataSanitizer;
import com.financeai.infrastructure.algorithm.KnapsackOptimizer;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PRINCÍPIOS SOLID APLICADOS: SRP, OCP, DIP
 *
 * PROBLEMAS IDENTIFICADOS E CORRIGIDOS:
 *
 * 1. CAMPO INUTILIZADO (SRP):
 *    staticWeights era inicializado no construtor mas NUNCA usado — o KnapsackOptimizer
 *    recalculava internamente via calculateDynamicWeights(). Campo removido.
 *
 * 2. CRIAÇÃO DE ESTRATÉGIA COM new (OCP + SRP):
 *    selectStrategy() instanciava BusinessStrategy/IndividualStrategy diretamente:
 *      return new BusinessStrategy(new BigDecimal("75.00"), BigDecimal.ZERO);
 *    Isso violava OCP (novo perfil = modificar o motor) e SRP (motor não deveria
 *    ser responsável por construção de objetos).
 *    SOLUÇÃO: TaxStrategyFactory injetada via construtor — motor delega a criação.
 *
 * 3. ACOPLAMENTO AO ALGORITMO DE PESOS (DIP):
 *    O motor chamava knapsackOptimizer.calculateDynamicWeights() — acoplando
 *    a lógica de cálculo ao otimizador. Se a origem dos pesos mudar (banco de dados,
 *    arquivo de configuração), o otimizador precisaria ser alterado.
 *    SOLUÇÃO: WeightStrategy injetada — o motor recebe pesos prontos, sem conhecer a origem.
 */
@Service
public class DefaultCalculationEngine implements CalculationEngine {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCalculationEngine.class);

    private final DataSanitizer sanitizer;
    private final KnapsackOptimizer knapsackOptimizer;
    private final WeightStrategy weightStrategy;
    private final TaxStrategyFactory taxStrategyFactory;

    public DefaultCalculationEngine(
            DataSanitizer sanitizer,
            KnapsackOptimizer knapsackOptimizer,
            WeightStrategy weightStrategy,
            TaxStrategyFactory taxStrategyFactory) {
        this.sanitizer = sanitizer;
        this.knapsackOptimizer = knapsackOptimizer;
        this.weightStrategy = weightStrategy;
        this.taxStrategyFactory = taxStrategyFactory;
    }

    @Override
    public FinancialDiagnostic calculate(String userId, List<Transaction> transactions, FinancialGoal goal) {
        logger.info("[Tenant: {}] Iniciando cálculo de diagnóstico financeiro", userId);

        if (transactions == null || transactions.isEmpty()) {
            logger.warn("[Tenant: {}] Nenhuma transação fornecida", userId);
            return createEmptyDiagnostic();
        }

        // Java 21: Record Pattern Matching para rastreabilidade por Tenant
        if (goal instanceof FinancialGoal(String name, BigDecimal target, LocalDate deadline, UserProfile profile)) {
            logger.info("[Tenant: {}] Analisando meta: {} (alvo: R$ {})", userId, name, target);
        }

        // 1. Sanitização e Limpeza de Dados
        List<Transaction> cleanTransactions = transactions.stream()
                .map(t -> new Transaction(
                        sanitizer.sanitizeDescription(t.description()),
                        t.amount(), t.date(), t.category(), t.isEssential(), t.priority()))
                .collect(Collectors.toList());

        BigDecimal currentBalance = calculateInitialBalance(cleanTransactions);

        // 2. Aplicação da Estratégia Fiscal via Factory — OCP aplicado
        // TaxStrategyFactory decide qual implementação usar com base no perfil.
        // Adicionar novo perfil = novo case na factory, sem tocar aqui.
        StrategyCalculation strategy = taxStrategyFactory.createFor(goal.profile());
        BigDecimal adjustedBalance = strategy.applyTaxDeduction(currentBalance);

        // 3. Cálculos de Taxas e Liquidez
        BigDecimal dailyRate = calculateAverageDailySpending(cleanTransactions);
        List<String> alerts = new ArrayList<>();

        Integer dayOfRisk = simulateLiquidity(adjustedBalance, dailyRate, alerts);
        BigDecimal monthlyGap = calculateMonthlyGap(adjustedBalance, goal);
        List<String> recommendations = new ArrayList<>();

        // 4. Otimização (Variação do Problema da Mochila)
        // WeightStrategy injetada fornece os pesos — motor não sabe se são sazonais ou do banco
        Map<String, Double> weights = weightStrategy.calculateWeights();
        List<Transaction> suggestedCuts = optimizeSavings(cleanTransactions, monthlyGap, weights);
        BigDecimal totalPotentialSaving = suggestedCuts.stream()
                .map(t -> t.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!suggestedCuts.isEmpty()) {
            recommendations.add("OTIMIZACAO_CORTES_DISPONIVEL");
            alerts.add(String.format("Sugestão de economia: R$ %.2f focando em itens não essenciais.",
                    totalPotentialSaving));
            logger.debug("[Tenant: {}] Oportunidades de otimização identificadas: R$ {}", userId, totalPotentialSaving);
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
            logger.info("[Tenant: {}] Revisão estrutural recomendada. Novo prazo sugerido: {}", userId, formattedDate);
        }

        logger.info("[Tenant: {}] Cálculo finalizado. Saldo ajustado: R$ {}", userId, adjustedBalance);

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

    // Pesos recebidos por parâmetro — desacoplado da origem (sazonal, banco, etc.)
    private List<Transaction> optimizeSavings(List<Transaction> transactions, BigDecimal gap,
                                              Map<String, Double> weights) {
        if (gap.compareTo(BigDecimal.ZERO) <= 0) return Collections.emptyList();

        List<Transaction> nonEssential = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0 && !t.isEssential())
                .collect(Collectors.toList());

        if (nonEssential.isEmpty()) return Collections.emptyList();

        return knapsackOptimizer.optimizeExpenses(nonEssential, gap, weights);
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
