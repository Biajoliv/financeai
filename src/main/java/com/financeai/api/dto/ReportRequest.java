package com.financeai.api.dto;

import com.financeai.domain.model.ReportInterval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
    @NotBlank(message = "O ID do usuário é obrigatório")
    String userId,
    
    @NotNull(message = "O intervalo do relatório deve ser informado")
    ReportInterval interval
) {}