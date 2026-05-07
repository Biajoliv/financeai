package com.financeai.infrastructure.simulation;

import java.math.BigDecimal;

public sealed interface StrategyCalculation 
    permits IndividualStrategy, BusinessStrategy {
    BigDecimal applyTaxDeduction(BigDecimal balance);
}
