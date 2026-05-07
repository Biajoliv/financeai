package com.financeai.domain.valueobject;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório") String name,
        @Email(message = "Email inválido") @NotBlank String email,
        @NotBlank @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String password
    ) {}

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record AuthResponse(
        String token,
        String userId,
        String name,
        String email
    ) {}

    public record ErrorResponse(
        int status,
        String error,
        String message
    ) {}
}
