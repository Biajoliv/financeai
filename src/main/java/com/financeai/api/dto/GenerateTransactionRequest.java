package com.financeai.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateTransactionRequest(
    @NotNull(message = "Quantidade de transações é obrigatória")
    @Min(value = 1, message = "Mínimo de 1 transação")
    @Max(value = 10, message = "Máximo de 10 transações por requisição")
    Integer count
) {}
