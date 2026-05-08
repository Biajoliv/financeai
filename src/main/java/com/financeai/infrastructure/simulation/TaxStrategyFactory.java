package com.financeai.infrastructure.simulation;

import com.financeai.domain.model.UserProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PRINCÍPIO SOLID APLICADO: Open/Closed Principle (OCP) + Factory Pattern
 *
 * PROBLEMA IDENTIFICADO:
 * Em DefaultCalculationEngine, o método selectStrategy() instanciava estratégias
 * diretamente com new dentro de um if:
 *
 *   if (goal.profile() == UserProfile.BUSINESS) return new BusinessStrategy(...);
 *   else return new IndividualStrategy();
 *
 * Isso violava OCP: qualquer novo perfil (MEI, LTDA, SA) exigiria modificar
 * o motor de cálculo, que não deveria saber nada sobre criação de estratégias.
 *
 * TAMBÉM VIOLA SRP: o motor de cálculo não é responsável por construir objetos —
 * essa é uma responsabilidade de uma factory.
 *
 * SOLUÇÃO — Factory Pattern:
 * A criação das estratégias é centralizada aqui. O switch expression do Java 21
 * garante que todos os casos do enum são tratados (exhaustive switch).
 *
 * EXTENSIBILIDADE:
 * Novo perfil de usuário = novo case aqui + nova classe de estratégia.
 * DefaultCalculationEngine não é tocado.
 */
@Component
public class TaxStrategyFactory {

    // Constantes extraídas para facilitar revisão e teste — valor de dedução PJ
    private static final BigDecimal PJ_FIXED_TAX = new BigDecimal("75.00");
    private static final BigDecimal PJ_VARIABLE_RATE = BigDecimal.ZERO;

    public StrategyCalculation createFor(UserProfile profile) {
        return switch (profile) {
            case BUSINESS  -> new BusinessStrategy(PJ_FIXED_TAX, PJ_VARIABLE_RATE);
            case INDIVIDUAL -> new IndividualStrategy();
        };
    }
}
