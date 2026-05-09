package com.financeai.api.dto;

/**
 * Campos MUTÁVEIS do perfil do usuário.
 * Campos imutáveis (email, password, cpf_cnpj, holder_type) rejeitados no service.
 * Todos os campos são opcionais — apenas os enviados são atualizados.
 */
public record UpdateProfileRequest(
    String name,
    Boolean hasOpenFinanceActive,
    String reportPreference,
    String postalCode,

    // Campos imutáveis — se enviados, serão rejeitados com erro 400
    String email,
    String password,
    String cpfCnpj,
    String holderType
) {}
