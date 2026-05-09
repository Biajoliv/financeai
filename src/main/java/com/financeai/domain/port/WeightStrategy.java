package com.financeai.domain.port;

import java.util.Map;

public interface WeightStrategy {
    Map<String, Double> calculateWeights();
}
