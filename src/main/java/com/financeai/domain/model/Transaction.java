package com.financeai.domain.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Record que representa uma transação financeira.
 * O campo priority é essencial para o algoritmo de otimização (Mochila).
 */
public record Transaction(
    @NotBlank(message = "A descrição não pode estar vazia")
    String description,

    @NotNull(message = "O valor é obrigatório")
    // Usamos PositiveOrZero pois transações de entrada são positivas e saídas tratamos o absoluto no motor
    BigDecimal amount,

    @NotNull(message = "A data é obrigatória")
    @PastOrPresent(message = "A data da transação não pode ser futura")
    LocalDate date,

    @NotBlank(message = "A categoria é obrigatória")
    String category,

    @NotNull(message = "A flag de essencial é obrigatória")
    Boolean isEssential,

    @NotNull(message = "A prioridade é obrigatória")
    @Min(value = 1, message = "A prioridade mínima é 1 (baixa importância)")
    @Max(value = 5, message = "A prioridade máxima é 5 (alta importância)")
    Integer priority
) {}