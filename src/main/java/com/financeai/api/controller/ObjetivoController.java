package com.financeai.api.controller;

import com.financeai.api.dto.AnalysisRequest;
import com.financeai.domain.port.InsightGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analise")
public class ObjetivoController {

    private final InsightGenerator insightGenerator;

    public ObjetivoController(InsightGenerator insightGenerator) {
        this.insightGenerator = insightGenerator;
    }

    @PostMapping
    public String analisar(@RequestBody AnalysisRequest request) {
        String prompt = """
                Analise os dados financeiros abaixo e gere um diagnóstico financeiro em JSON válido.

                Objetivo financeiro:
                %s

                Quantidade de transações:
                %d
                """.formatted(
                request.getGoal(),
                request.getTransactions() == null ? 0 : request.getTransactions().size()
        );

        return insightGenerator.gerar(prompt);
    }

    @GetMapping("/teste")
    public String teste() {
        return "Backend funcionando";
    }
}