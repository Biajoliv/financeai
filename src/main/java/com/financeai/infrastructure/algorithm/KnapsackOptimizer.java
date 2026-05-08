package com.financeai.infrastructure.algorithm;

import com.financeai.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Knapsack 0/1 algorithm implementation using Dynamic Programming.
 * 
 * This algorithm optimizes which expenses to cut to meet a monthly savings goal,
 * while respecting category weights and expense priorities.
 * 
 * Time Complexity: O(n * W) where n = number of items, W = knapsack capacity
 * Space Complexity: O(n * W)
 */
@Service
public class KnapsackOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(KnapsackOptimizer.class);

    /**
     * Solve the 0/1 knapsack problem to find optimal expense cuts.
     * 
     * @param transactions Non-essential transactions to consider cutting
     * @param monthlyGap   Maximum amount (in cents) that can be saved
     * @param weights      Category weights (impact on priority)
     * @return List of transactions recommended for cutting, sorted by efficiency
     */
    public List<Transaction> optimizeExpenses(
            List<Transaction> transactions,
            BigDecimal monthlyGap,
            Map<String, Double> weights) {

        if (transactions == null || transactions.isEmpty() || monthlyGap.compareTo(BigDecimal.ZERO) <= 0) {
            logger.debug("No optimization needed: empty list or zero capacity");
            return Collections.emptyList();
        }

        // Convert capacity to integer cents (avoid float precision issues)
        int capacity = monthlyGap.setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).intValue();
        
        if (capacity > 100000) {
            logger.warn("Capacidade muito alta ({}). Limitando para 100.000 centavos para evitar estouro de memória.", capacity);
            capacity = 100000;
        }

        if (capacity <= 0) {
            return Collections.emptyList();
        }

        int n = transactions.size();
        
        // DP table: dp[i][w] = maximum value achievable with first i items and capacity w
        double[][] dp = new double[n + 1][capacity + 1];
        
        // Item values (efficiency scores)
        double[] values = new double[n];
        int[] weights_int = new int[n];

        // Prepare items: convert to integer weights (cents) and calculate efficiencies
        for (int i = 0; i < n; i++) {
            Transaction tx = transactions.get(i);
            
            // Weight = transaction amount in cents
            int weight = tx.amount()
                    .setScale(2, RoundingMode.DOWN)
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();
            
            if (weight > capacity) {
                weight = capacity;
            } else if (weight <= 0) {
                weight = 1; 
            }
            
            weights_int[i] = weight;
            
            // Value = efficiency of cutting this expense
            double categoryWeight = weights.getOrDefault(tx.category(), 1.0);
            double priority = Math.max(1.0, tx.priority()); // Avoid division issues
            
            // Efficiency = (amount / category_weight) / priority^2
            // Higher efficiency = more valuable to cut
            values[i] = (tx.amount().doubleValue() / categoryWeight) / (priority * priority);
            
            logger.debug("Item {}: weight={} cents, value={}, category={}, priority={}",
                    i, weight, values[i], tx.category(), priority);
        }

        // Fill DP table using 0/1 knapsack logic
        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                // Option 1: Don't take item i-1
                dp[i][w] = dp[i - 1][w];
                
                // Option 2: Take item i-1 (if it fits)
                if (weights_int[i - 1] <= w) {
                    double valueWithItem = dp[i - 1][w - weights_int[i - 1]] + values[i - 1];
                    dp[i][w] = Math.max(dp[i][w], valueWithItem);
                }
            }
        }

        logger.debug("DP table filled. Max value: {}", dp[n][capacity]);

        // Backtrack to find which items to include
        List<Transaction> result = new ArrayList<>();
        int remainingCapacity = capacity;
        
        for (int i = n; i > 0 && remainingCapacity > 0; i--) {
            // Check if this item was included in optimal solution
            if (dp[i][remainingCapacity] != dp[i - 1][remainingCapacity]) {
                Transaction tx = transactions.get(i - 1);
                result.add(tx);
                remainingCapacity -= weights_int[i - 1];
                logger.debug("Item {} included in solution", i - 1);
            }
        }

        // Sort by efficiency (descending) for better presentation
        result.sort((t1, t2) -> {
            double w1 = weights.getOrDefault(t1.category(), 1.0);
            double w2 = weights.getOrDefault(t2.category(), 1.0);
            double eff1 = (t1.amount().doubleValue() / w1) / Math.pow(t1.priority(), 2);
            double eff2 = (t2.amount().doubleValue() / w2) / Math.pow(t2.priority(), 2);
            return Double.compare(eff2, eff1); // Descending
        });

        logger.info("Knapsack optimization complete: {} items selected, {} cents saved",
                result.size(), capacity - remainingCapacity);

        return result;
    }

    /**
     * Calculate dynamic category weights based on current month.
     * Accounts for seasonal variations (e.g., electricity higher in summer/winter).
     * 
     * @return Map of category -> weight multiplier
     */
    public Map<String, Double> calculateDynamicWeights() {
        int month = LocalDate.now().getMonthValue();
        Map<String, Double> weights = new HashMap<>();

        // Energy costs are higher in summer (Dec-Mar) and winter (Jun-Aug in Southern Hemisphere)
        // Using Brazil's climate as reference
        if (month >= 12 || month <= 3 || (month >= 6 && month <= 8)) {
            weights.put("HOUSING", 1.5); // Electricity increases
        } else {
            weights.put("HOUSING", 1.0);
        }

        // Entertainment is more expensive during holidays
        if (month == 1 || month == 6 || month == 7 || month == 12) {
            weights.put("ENTERTAINMENT", 0.8); // Less penalty (expected)
        } else {
            weights.put("ENTERTAINMENT", 0.5); // More penalty (optional)
        }

        // Back to school (February) - education more important
        if (month == 2) {
            weights.put("EDUCATION", 2.5);
        } else {
            weights.put("EDUCATION", 2.0);
        }

        // Health is always important
        weights.put("HEALTH", 2.5);

        // Food is essential
        weights.put("FOOD", 2.0);

        // Transport varies with fuel prices (simplified)
        weights.put("TRANSPORT", 1.2);

        // Subscriptions/other
        weights.put("SUBSCRIPTION", 0.7);
        weights.put("OTHER", 1.0);

        logger.info("Dynamic weights calculated for month {}: {}", month, weights);
        return weights;
    }
}
