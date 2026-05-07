package com.financeai.domain.model;

import java.util.List;

public record FinancialAnalysisResponse(
    FinancialDiagnostic diagnostic,
    List<ScenarioResult> scenarios
) {}