package com.financeai.application.usecase;

import com.financeai.domain.entity.SimulationRecord;
import com.financeai.domain.model.*;
import com.financeai.domain.port.CalculationEngine;
import com.financeai.infrastructure.persistence.SimulationRecordRepository;
import com.financeai.infrastructure.persistence.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PRINCÍPIO SOLID APLICADO: Single Responsibility Principle (SRP)
 *
 * PROBLEMA IDENTIFICADO:
 * Este use case retornava apenas List<ScenarioResult>, forçando o controller
 * a chamar engine.calculate() NOVAMENTE para obter o diagnóstico:
 *
 *   // ANTES — controller chamava o motor DUAS VEZES para a mesma requisição:
 *   List<ScenarioResult> scenarios = simulateUseCase.execute(userId, transactions, goal);
 *   FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal); // 2ª vez!
 *
 * Isso: (a) desperdiçava CPU calculando o mesmo diagnóstico duas vezes;
 *        (b) acoplava o controller ao CalculationEngine, violando SRP —
 *            controller não deve orquestrar cálculos de domínio.
 *
 * SOLUÇÃO:
 * O use case agora retorna SimulationResult (record Java 21) que agrupa
 * diagnóstico + cenários. O controller chama apenas este use case e desempacota
 * o resultado — sem acesso direto ao motor.
 *
 * BENEFÍCIO ADICIONAL:
 * O diagnóstico é calculado uma única vez e reutilizado nos três cenários,
 * garantindo consistência (RED/YELLOW/GREEN partem do mesmo estado base).
 */
@Service
public class SimulateScenariosUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SimulateScenariosUseCase.class);

    private final CalculationEngine engine;
    private final SimulationRecordRepository simulationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SimulateScenariosUseCase(CalculationEngine engine,
                                    SimulationRecordRepository simulationRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper) {
        this.engine = engine;
        this.simulationRepository = simulationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public SimulationResult execute(String userEmail, List<Transaction> transactions, FinancialGoal goal) {
        String userId = userEmail; // email é o identificador no JWT
        // Motor chamado UMA única vez — diagnóstico reutilizado nos cenários abaixo
        FinancialDiagnostic baseDiagnostic = engine.calculate(userId, transactions, goal);

        BigDecimal currentSurplus = calculateMonthlySurplus(transactions, BigDecimal.ZERO);

        // Cenário RECOMENDADO (Green): melhor caso com otimização da mochila
        BigDecimal potentialSavings = calculatePotentialSavings(transactions);
        BigDecimal recommendedSurplus = calculateMonthlySurplus(transactions, potentialSavings);
        BigDecimal recommendedBalance = baseDiagnostic.projectedBalance30Days().add(potentialSavings);

        // Cenário ATUAL (Yellow): inércia — comportamento real hoje
        BigDecimal currentBalance = baseDiagnostic.projectedBalance30Days();

        // Cenário PESSIMISTA (Red): inércia + 15% de degradação
        BigDecimal pessimisticMultiplier = new BigDecimal("0.85");
        BigDecimal pessimisticSurplus = currentSurplus.multiply(pessimisticMultiplier);
        BigDecimal pessimisticBalance = currentBalance.multiply(pessimisticMultiplier);

        List<ScenarioResult> scenarios = List.of(
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

        SimulationResult result = new SimulationResult(baseDiagnostic, scenarios);

        // Persiste a simulação no banco associada ao usuário
        persistSimulation(userEmail, goal, result);

        return result;
    }

    private void persistSimulation(String userEmail, FinancialGoal goal, SimulationResult result) {
        try {
            // Resolve userId do banco pelo email
            UUID userId = userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                    .map(u -> u.getId())
                    .orElse(null);

            if (userId == null) return;

            SimulationRecord record = SimulationRecord.builder()
                    .userId(userId)
                    .goalData(objectMapper.writeValueAsString(goal))
                    .scenarioResults(objectMapper.writeValueAsString(result.scenarios()))
                    .diagnosticData(objectMapper.writeValueAsString(result.diagnostic()))
                    .build();

            simulationRepository.save(record);
            logger.debug("[Tenant: {}] Simulação persistida com id={}", userEmail, record.getId());
        } catch (Exception e) {
            // Falha na persistência não deve quebrar o fluxo principal
            logger.error("[Tenant: {}] Erro ao persistir simulação: {}", userEmail, e.getMessage());
        }
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
        // Sobra mensal negativa ou zero = meta inviável (99 como sentinela de inviabilidade)
        if (monthlySurplus.compareTo(BigDecimal.ZERO) <= 0) return 99;

        BigDecimal gap = goal.targetAmount().subtract(currentBalance);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) return 0;

        return gap.divide(monthlySurplus, 0, RoundingMode.CEILING).intValue();
    }
}
