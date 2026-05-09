package com.financeai.domain.model;

import java.util.List;

/**
 * PRINCÍPIO SOLID APLICADO: Single Responsibility Principle (SRP) — eliminação de
 * duplicidade de processamento.
 *
 * PROBLEMA IDENTIFICADO:
 * Em FinancialAnalysisController.simulate(), o motor de cálculo (engine.calculate())
 * era invocado DUAS VEZES para a mesma requisição:
 *
 *   1ª chamada: dentro de SimulateScenariosUseCase.execute()
 *   2ª chamada: diretamente no controller via engine.calculate()
 *
 * Isso duplicava processamento (CPU, tempo) e acoplava o controller à interface
 * interna do motor — violando SRP (o controller não deve orquestrar cálculos).
 *
 * SOLUÇÃO — Record Java 21:
 * SimulateScenariosUseCase agora retorna SimulationResult, que agrupa o diagnóstico
 * financeiro e os cenários em um único objeto. O controller usa apenas o use case,
 * sem acesso direto ao CalculationEngine.
 *
 * BÔNUS — Imutabilidade:
 * Record Java 21 é imutável por padrão, sem getters boilerplate.
 */
public record SimulationResult(
    FinancialDiagnostic diagnostic,
    List<ScenarioResult> scenarios
) {}
