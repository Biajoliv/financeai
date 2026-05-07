package com.financeai.infrastructure.utils;

import org.springframework.stereotype.Component;

@Component
public class DataSanitizer {
    /**
     * Limpa descrições de transações, removendo IDs numéricos longos e caracteres especiais.
     */
    public String sanitizeDescription(String rawDesc) {
        if (rawDesc == null) return "TRANSACAO_NAO_IDENTIFICADA";
        
        return rawDesc.toUpperCase()
            .replaceAll("\\d{5,}", "") // Remove protocolos/IDs longos
            .replaceAll("[^A-ZÁÉÍÓÚÃÕÇ\\s]", "") // Mantém apenas letras e espaços
            .replaceAll("\\s+", " ") // Remove espaços duplos
            .trim();
    }
}