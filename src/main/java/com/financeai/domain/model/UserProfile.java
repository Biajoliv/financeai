package com.financeai.domain.model;

/**
 * Define a natureza jurídica do usuário para aplicação de regras fiscais.
 */
public enum UserProfile {
    INDIVIDUAL,   // Pessoa Física (PF)
    BUSINESS      // Pessoa Jurídica (PJ) - Engloba MEI e outras empresas
}