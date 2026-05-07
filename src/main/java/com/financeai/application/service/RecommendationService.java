package com.financeai.application.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Map<String, String> KNOWLEDGE_BASE = Map.of(
        "OTIMIZACAO_CORTES_DISPONIVEL", "Identificamos gastos supérfluos. Priorize cancelar assinaturas que você não usou nos últimos 30 dias.",
        "REVISAO_ESTRUTURAL_NECESSARIA", "Sua meta está distante da realidade atual. Considere aumentar sua renda extra ou reduzir custos fixos como moradia.",
        "WAITING_FOR_DATA", "Aguardando mais transações para gerar um insight preciso."
    );

    public List<String> getHumanReadableTips(List<String> chunks) {
        return chunks.stream()
            .map(chunk -> KNOWLEDGE_BASE.getOrDefault(chunk, "Continue monitorando seus gastos para novos insights."))
            .collect(Collectors.toList());
    }
}