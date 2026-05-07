package com.financeai.api.dto;

import com.financeai.domain.model.FinancialDiagnostic;

public record FinancialReportResponse(
    FinancialDiagnostic diagnostic,
    String advice
) {}