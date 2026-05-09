package com.financeai.api.dto;

import java.util.List;

public record AnaliseResponse(
        String diagnostico,
        String nivel_risco,
        List<String> principais_problemas,
        List<String> recomendacoes,
        List<String> proximos_passos,
        List<String> alertas,
        double confianca
) {
}

