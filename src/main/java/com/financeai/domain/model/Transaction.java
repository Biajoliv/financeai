package com.financeai.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Record que representa uma transação financeira para processamento pelo motor de análise.
 * 
 * NOTA: Esta é a representação de domínio usada para cálculos e transferência de dados.
 * Para persistência no banco, veja {@link com.financeai.domain.entity.Transaction}.
 * 
 * O campo priority é essencial para o algoritmo de otimização (Mochila Greedy).
 */
public record Transaction(
    @NotBlank(message = "A descrição não pode estar vazia")
    @JsonProperty("description")
    String description,

    @NotNull(message = "O valor é obrigatório")
    @JsonProperty("amount")
    BigDecimal amount,

    @NotNull(message = "A data é obrigatória")
    @PastOrPresent(message = "A data da transação não pode ser futura")
    @JsonProperty("date")
    LocalDate date,

    @NotBlank(message = "A categoria é obrigatória")
    @JsonProperty("category")
    String category,

    @NotNull(message = "A flag de essencial é obrigatória")
    @JsonProperty("isEssential")
    Boolean isEssential,

    @NotNull(message = "A prioridade é obrigatória")
    @Min(value = 1, message = "A prioridade mínima é 1 (baixa importância)")
    @Max(value = 5, message = "A prioridade máxima é 5 (alta importância)")
    @JsonProperty("priority")
    Integer priority
) {}