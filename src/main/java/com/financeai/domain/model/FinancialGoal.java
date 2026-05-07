package com.financeai.domain.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialGoal(
    @NotBlank(message = "O nome do objetivo é obrigatório")
    String name,

    @NotNull(message = "O valor total do objetivo é obrigatório")
    @Positive(message = "O valor do objetivo deve ser maior que zero")
    BigDecimal targetAmount,

    @NotNull(message = "A data limite é obrigatória")
    @Future(message = "A data limite deve ser uma data futura")
    LocalDate deadline,

    @NotNull(message = "O perfil do usuário é obrigatório")
    UserProfile profile
) {}