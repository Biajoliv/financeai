package com.financeai.api.dto;

import com.financeai.domain.model.ReportInterval;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
    @NotNull(message = "O intervalo do relatório deve ser informado")
    ReportInterval interval
) {}