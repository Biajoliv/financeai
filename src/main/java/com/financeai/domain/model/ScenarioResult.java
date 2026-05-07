package com.financeai.domain.model;

import java.math.BigDecimal;

public record ScenarioResult(
    BigDecimal projectedBalance,
    Integer monthsToGoal,
    String statusColor // RED, GREEN, YELLOW
) {}