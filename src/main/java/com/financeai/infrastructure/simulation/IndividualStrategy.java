package com.financeai.infrastructure.simulation;

import java.math.BigDecimal;

public final class IndividualStrategy implements StrategyCalculation {
    @Override
    public BigDecimal applyTaxDeduction(BigDecimal balance) {
        // Para Pessoa Física, não há dedução automática de impostos sobre o saldo
        return balance;
    }
}