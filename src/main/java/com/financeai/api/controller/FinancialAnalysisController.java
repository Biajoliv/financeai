package com.financeai.api.controller;

import com.financeai.api.dto.*;
import com.financeai.domain.model.*;
import com.financeai.application.usecase.*;
import com.financeai.infrastructure.simulation.DefaultCalculationEngine;
import com.financeai.application.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api/analysis")
public class FinancialAnalysisController {

    private final SimulateScenariosUseCase simulateUseCase;
    private final DefaultCalculationEngine engine;
    private final RecommendationService recommendationService;
    private final AnalyzeGoalUseCase analyzeGoalUseCase;
    private final ReportUseCase reportUseCase;

    public FinancialAnalysisController(SimulateScenariosUseCase simulateUseCase, 
                                     DefaultCalculationEngine engine,
                                     RecommendationService recommendationService, 
                                     AnalyzeGoalUseCase analyzeGoalUseCase, 
                                     ReportUseCase reportUseCase) {
        this.simulateUseCase = simulateUseCase;
        this.engine = engine;
        this.recommendationService = recommendationService;
        this.analyzeGoalUseCase = analyzeGoalUseCase;
        this.reportUseCase = reportUseCase;
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<FinancialAnalysisResponse>> simulate(@Valid @RequestBody AnalysisRequest request) {
        String userId = "current-user-id";
        List<ScenarioResult> scenarios = simulateUseCase.execute(userId, request.getTransactions(), request.getGoal());
        FinancialDiagnostic diagnostic = engine.calculate(userId, request.getTransactions(), request.getGoal());
        List<String> humanTips = recommendationService.getHumanReadableTips(diagnostic.recommendationChunks());
        
        List<String> combinedAlerts = new ArrayList<>(diagnostic.alerts());
        combinedAlerts.addAll(humanTips);

        FinancialDiagnostic finalDiagnostic = new FinancialDiagnostic(
            diagnostic.currentBalance(),
            diagnostic.projectedBalance30Days(),
            diagnostic.daysUntilNegativeBalance(),
            combinedAlerts,
            diagnostic.recommendationChunks()
        );

        FinancialAnalysisResponse data = new FinancialAnalysisResponse(finalDiagnostic, scenarios);
        
        return ResponseEntity.ok(ApiResponse.success("Simulação processada com sucesso.", data));
    }

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> generateReport(@Valid @RequestBody ReportRequest request) {
        var response = reportUseCase.generatePeriodReport(request.userId(), request.interval());
        return ResponseEntity.ok(ApiResponse.success("Relatório gerado com sucesso", response));
    }

    @GetMapping("/report/periods")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableReportPeriods() {
        List<String> periods = Arrays.stream(ReportInterval.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Períodos de relatório disponíveis", periods));
    }

    @GetMapping("/backpack-details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackpackDetails() {
        return ResponseEntity.ok(ApiResponse.success("Detalhes do algoritmo de otimização", 
            Map.of("algorithm", "Knapsack 0/1", "complexity", "O(nW)")));
    }

    @GetMapping("/config/weights")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getWeights() {
        return ResponseEntity.ok(ApiResponse.success("Pesos de prioridade atuais", 
            Map.of("ESSENTIAL", 10, "IMPORTANT", 5, "OPTIONAL", 1)));
    }

    @GetMapping("/config/tax-rules")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getTaxRules() {
        return ResponseEntity.ok(ApiResponse.success("Regras de tributação simuladas", 
            Map.of("IOF", 0.0038, "IR_SHORT_TERM", 0.225)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success("Status do Motor", "OPERATIONAL - Java 21 Virtual Threads Enabled"));
    }
}