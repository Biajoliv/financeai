package com.financeai.infrastructure.simulation;

import com.financeai.domain.service.WeightStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * PRINCÍPIO SOLID APLICADO: Single Responsibility Principle (SRP) + Strategy Pattern
 *
 * PROBLEMA IDENTIFICADO:
 * calculateDynamicWeights() existia dentro de KnapsackOptimizer misturando
 * dois contextos: algoritmo de otimização (mochila) e regras de negócio sazonais.
 * Isso fazia o otimizador assumir duas responsabilidades distintas.
 *
 * SOLUÇÃO:
 * Toda a lógica sazonal foi extraída para esta classe, que implementa WeightStrategy.
 * KnapsackOptimizer agora recebe um Map<String, Double> pronto — desconhece
 * completamente a origem dos pesos.
 *
 * EXTENSIBILIDADE (OCP):
 * Para usar pesos do banco de dados, basta criar DatabaseWeightStrategy implementando
 * WeightStrategy e configurar qual implementação o Spring deve injetar via @Primary
 * ou @Qualifier — sem alterar nenhuma linha do KnapsackOptimizer.
 */
@Component
public class SeasonalWeightStrategy implements WeightStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SeasonalWeightStrategy.class);

    @Override
    public Map<String, Double> calculateWeights() {
        int month = LocalDate.now().getMonthValue();
        Map<String, Double> weights = new HashMap<>();

        // Verão (dez–mar) e inverno (jun–ago) elevam custo de energia elétrica
        weights.put("HOUSING", isHighEnergySeason(month) ? 1.5 : 1.0);

        // Em meses de feriado, entretenimento é esperado: penalidade reduzida
        weights.put("ENTERTAINMENT", isHolidayMonth(month) ? 0.8 : 0.5);

        // Volta às aulas (fev) eleva importância da categoria educação
        weights.put("EDUCATION", month == 2 ? 2.5 : 2.0);

        weights.put("HEALTH", 2.5);
        weights.put("FOOD", 2.0);
        weights.put("TRANSPORT", 1.2);
        weights.put("SUBSCRIPTION", 0.7);
        weights.put("OTHER", 1.0);

        logger.debug("Pesos sazonais calculados para o mês {}: {}", month, weights);
        return weights;
    }

    private boolean isHighEnergySeason(int month) {
        return month >= 12 || month <= 3 || (month >= 6 && month <= 8);
    }

    private boolean isHolidayMonth(int month) {
        return month == 1 || month == 6 || month == 7 || month == 12;
    }
}
