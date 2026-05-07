package com.financeai.infrastructure.simulation;

import java.math.BigDecimal;

public final class BusinessStrategy implements StrategyCalculation {
    private final BigDecimal fixedTax;
    private final BigDecimal variableRate;

    public BusinessStrategy(BigDecimal fixedTax, BigDecimal variableRate) {
        this.fixedTax = fixedTax;
        this.variableRate = variableRate;
    }

    @Override
    public BigDecimal applyTaxDeduction(BigDecimal amount) {
        BigDecimal variableDeduction = amount.multiply(variableRate);
        return amount.subtract(fixedTax).subtract(variableDeduction);
    }
}