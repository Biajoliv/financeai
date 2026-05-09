package com.financeai.infrastructure.algorithm;

import com.financeai.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * PRINCÍPIO SOLID APLICADO: Single Responsibility Principle (SRP)
 *
 * PROBLEMA IDENTIFICADO:
 * Esta classe tinha duas responsabilidades distintas:
 *
 * 1. Algoritmo de otimização 0/1 Knapsack com DP — sua responsabilidade real
 * 2. Lógica de negócio de pesos sazonais (calculateDynamicWeights) — responsabilidade
 *    que deveria pertencer a uma WeightStrategy
 *
 * Misturar as duas tornava impossível substituir a lógica de pesos sem alterar o
 * algoritmo, e dificultava testes unitários (impossível testar o knapsack com pesos
 * arbitrários sem refatoração).
 *
 * SOLUÇÃO:
 * calculateDynamicWeights() foi movido para SeasonalWeightStrategy.
 * Esta classe agora faz exatamente uma coisa: receber itens + capacidade + pesos
 * e retornar a seleção ótima via programação dinâmica.
 *
 * Knapsack 0/1 com Dynamic Programming.
 * Time Complexity: O(n * W) | Space Complexity: O(n * W)
 */
@Service
public class KnapsackOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(KnapsackOptimizer.class);

    /**
     * Resolve o problema 0/1 Knapsack para encontrar os cortes ótimos de despesas.
     *
     * @param transactions Transações não-essenciais candidatas a corte
     * @param monthlyGap   Capacidade máxima de economia (em reais)
     * @param weights      Pesos por categoria fornecidos externamente (WeightStrategy)
     * @return Lista de transações recomendadas para corte, ordenadas por eficiência
     */
    public List<Transaction> optimizeExpenses(
            List<Transaction> transactions,
            BigDecimal monthlyGap,
            Map<String, Double> weights) {

        if (transactions == null || transactions.isEmpty() || monthlyGap.compareTo(BigDecimal.ZERO) <= 0) {
            logger.debug("Nenhuma otimização necessária: lista vazia ou capacidade zero");
            return Collections.emptyList();
        }

        // Converte capacidade para centavos inteiros (evita imprecisão de ponto flutuante)
        int capacity = monthlyGap.setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).intValue();

        if (capacity > 100000) {
            // Limite de segurança para evitar estouro de memória na DP table
            logger.warn("Capacidade muito alta ({}). Limitando para 100.000 centavos.", capacity);
            capacity = 100000;
        }

        if (capacity <= 0) return Collections.emptyList();

        int n = transactions.size();
        double[][] dp = new double[n + 1][capacity + 1];
        double[] values = new double[n];
        int[] weightsInt = new int[n];

        for (int i = 0; i < n; i++) {
            Transaction tx = transactions.get(i);

            int weight = tx.amount()
                    .abs()
                    .setScale(2, RoundingMode.DOWN)
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            weight = Math.min(Math.max(weight, 1), capacity);
            weightsInt[i] = weight;

            // Eficiência = quanto vale cortar este gasto, ponderado pela categoria e prioridade
            double categoryWeight = weights.getOrDefault(tx.category(), 1.0);
            double priority = Math.max(1.0, tx.priority());
            values[i] = (tx.amount().abs().doubleValue() / categoryWeight) / (priority * priority);

            logger.debug("Item {}: weight={} cents, value={}, category={}", i, weight, values[i], tx.category());
        }

        // Preenchimento da tabela DP — 0/1 Knapsack clássico
        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w];
                if (weightsInt[i - 1] <= w) {
                    double valueWithItem = dp[i - 1][w - weightsInt[i - 1]] + values[i - 1];
                    dp[i][w] = Math.max(dp[i][w], valueWithItem);
                }
            }
        }

        // Backtracking para identificar quais itens compõem a solução ótima
        List<Transaction> result = new ArrayList<>();
        int remainingCapacity = capacity;

        for (int i = n; i > 0 && remainingCapacity > 0; i--) {
            if (dp[i][remainingCapacity] != dp[i - 1][remainingCapacity]) {
                result.add(transactions.get(i - 1));
                remainingCapacity -= weightsInt[i - 1];
            }
        }

        // Ordenação por eficiência decrescente para melhor apresentação
        result.sort((t1, t2) -> {
            double w1 = weights.getOrDefault(t1.category(), 1.0);
            double w2 = weights.getOrDefault(t2.category(), 1.0);
            double eff1 = (t1.amount().abs().doubleValue() / w1) / Math.pow(t1.priority(), 2);
            double eff2 = (t2.amount().abs().doubleValue() / w2) / Math.pow(t2.priority(), 2);
            return Double.compare(eff2, eff1);
        });

        logger.info("Knapsack concluído: {} itens selecionados", result.size());
        return result;
    }
}
