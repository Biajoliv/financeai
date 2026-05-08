package com.financeai.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.financeai.api.dto.*;
import com.financeai.domain.model.*;
import com.financeai.domain.entity.Diagnostic;
import com.financeai.domain.repository.DiagnosticRepository;
import com.financeai.application.usecase.*;
import com.financeai.domain.service.CalculationEngine;
import com.financeai.application.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Financial Analysis", description = "Motor de análise e simulação financeira")
public class FinancialAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(FinancialAnalysisController.class);
    private final SimulateScenariosUseCase simulateUseCase;
    private final CalculationEngine engine;
    private final RecommendationService recommendationService;
    private final AnalyzeGoalUseCase analyzeGoalUseCase;
    private final ReportUseCase reportUseCase;
    private final DiagnosticRepository diagnosticRepository;

    public FinancialAnalysisController(SimulateScenariosUseCase simulateUseCase, 
                                     CalculationEngine engine,
                                     RecommendationService recommendationService, 
                                     AnalyzeGoalUseCase analyzeGoalUseCase, 
                                     ReportUseCase reportUseCase,
                                     DiagnosticRepository diagnosticRepository) {
        this.simulateUseCase = simulateUseCase;
        this.engine = engine;
        this.recommendationService = recommendationService;
        this.analyzeGoalUseCase = analyzeGoalUseCase;
        this.reportUseCase = reportUseCase;
        this.diagnosticRepository = diagnosticRepository;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simular cenários financeiros", 
               description = "Processa transações do usuário e retorna diagnóstico com múltiplos cenários (pessimista, atual, otimista)")
    public ResponseEntity<ApiResponse<FinancialAnalysisResponse>> simulate(@Valid @RequestBody AnalysisRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userId = userDetails.getUsername();

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
    @Operation(summary = "Gerar relatório periódico", 
               description = "Gera relatório de análise financeira para o período solicitado (diário, semanal, mensal)")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> generateReport(@Valid @RequestBody ReportRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userId = userDetails.getUsername();
        var response = reportUseCase.generatePeriodReport(userId, request.interval());
        return ResponseEntity.ok(ApiResponse.success("Relatório gerado com sucesso", response));
    }

    @GetMapping("/report/periods")
    @Operation(summary = "Listar períodos disponíveis", 
               description = "Retorna lista de períodos suportados para relatórios")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableReportPeriods(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        List<String> periods = Arrays.stream(ReportInterval.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Períodos de relatório disponíveis", periods));
    }

    @DeleteMapping("/{diagnosticId}")
    @Operation(summary = "Deletar relatório de diagnóstico", 
               description = "Remove um relatório de diagnóstico específico do usuário autenticado (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteDiagnostic(
            @PathVariable String diagnosticId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        logger.info("[Tenant: {}] Iniciando exclusão de diagnóstico: {}", userId, diagnosticId);
        
        try {
            Diagnostic diagnostic = diagnosticRepository
                    .findByIdAndUserIdAndNotDeleted(diagnosticId, userId)
                    .orElseThrow(() -> {
                        logger.warn("[Tenant: {}] Diagnóstico não encontrado ou não autorizado: {}", userId, diagnosticId);
                        return new IllegalArgumentException("Relatório não encontrado ou não autorizado");
                    });
            
            diagnostic.setDeletedAt(LocalDateTime.now());
            diagnosticRepository.save(diagnostic);
            
            logger.info("[Tenant: {}] Diagnóstico deletado com sucesso: {}", userId, diagnosticId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("[Tenant: {}] Erro ao deletar diagnóstico: {}", userId, e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erro ao deletar relatório. Tente novamente mais tarde."));
        }
    }

    @GetMapping("/backpack-details")
    @Operation(summary = "Detalhes do algoritmo de otimização", 
               description = "Retorna informações técnicas sobre o algoritmo 0/1 knapsack com programação dinâmica")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackpackDetails(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(ApiResponse.success("Detalhes do algoritmo de otimização", 
            Map.of(
                "algorithm", "Knapsack 0/1 (Dynamic Programming)",
                "complexity_time", "O(n * W)",
                "complexity_space", "O(n * W)",
                "approach", "Selects optimal expenses to cut based on efficiency score and dynamic category weights"
            )));
    }

    @GetMapping("/config/weights")
    @Operation(summary = "Obter pesos de prioridade", 
               description = "Retorna pesos utilizados no algoritmo de otimização para categorizar gastos")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getWeights(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(ApiResponse.success("Pesos de prioridade atuais", 
            Map.of("ESSENTIAL", 10, "IMPORTANT", 5, "OPTIONAL", 1)));
    }

    @GetMapping("/config/tax-rules")
    @Operation(summary = "Obter regras de tributação", 
               description = "Retorna regras de tributação simuladas para cálculos de projeção")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getTaxRules(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        return ResponseEntity.ok(ApiResponse.success("Regras de tributação simuladas", 
            Map.of("IOF", 0.0038, "IR_SHORT_TERM", 0.225)));
    }

    @GetMapping("/status")
    @Operation(summary = "Status operacional do motor", 
               description = "Verifica se o motor de análise está operacional")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success("Status do Motor", "OPERATIONAL"));
    }
}