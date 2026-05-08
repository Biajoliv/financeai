package com.financeai.domain.service;

import java.util.Map;

/**
 * PRINCÍPIO SOLID APLICADO: Open/Closed Principle (OCP) + Strategy Pattern
 *
 * PROBLEMA IDENTIFICADO:
 * A lógica de cálculo de pesos por categoria estava hardcoded dentro de
 * KnapsackOptimizer.calculateDynamicWeights(). Isso violava dois princípios:
 *
 * 1. SRP: O otimizador (algoritmo) misturava responsabilidade de contexto
 *    de negócio (sazonalidade, preferências) com a responsabilidade de otimização.
 *
 * 2. OCP: Para adicionar uma nova lógica de pesos (ex: pesos do banco de dados,
 *    pesos por perfil de usuário), seria necessário modificar KnapsackOptimizer.
 *
 * SOLUÇÃO — Strategy Pattern:
 * Esta interface define o contrato. Implementações concretas encapsulam cada
 * variação de cálculo de pesos:
 * - SeasonalWeightStrategy: pesos baseados em sazonalidade (atual)
 * - DatabaseWeightStrategy: pesos configurados pelo usuário no banco (futuro)
 * - ProfileWeightStrategy: pesos por perfil PF/PJ (futuro)
 *
 * BENEFÍCIO:
 * KnapsackOptimizer recebe um Map<String, Double> pronto — não sabe e não precisa
 * saber como foi calculado. Novos contextos = nova implementação, sem tocar no algo.
 */
public interface WeightStrategy {
    Map<String, Double> calculateWeights();
}
