package com.financeai.api.controller;

import com.financeai.api.dto.*;
import com.financeai.application.service.UserService;
import com.financeai.domain.entity.Diagnostic;
import com.financeai.domain.entity.SimulationRecord;
import com.financeai.domain.model.*;
import com.financeai.infrastructure.persistence.DiagnosticRepository;
import com.financeai.infrastructure.persistence.SimulationRecordRepository;
import com.financeai.application.usecase.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Financial Analysis", description = "Motor de análise e simulação financeira")
public class FinancialAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(FinancialAnalysisController.class);

    private final SimulateScenariosUseCase simulateUseCase;
    private final AnalyzeGoalUseCase analyzeGoalUseCase;
    private final ReportUseCase reportUseCase;
    private final DiagnosticRepository diagnosticRepository;
    private final SimulationRecordRepository simulationRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public FinancialAnalysisController(
            SimulateScenariosUseCase simulateUseCase,
            AnalyzeGoalUseCase analyzeGoalUseCase,
            ReportUseCase reportUseCase,
            DiagnosticRepository diagnosticRepository,
            SimulationRecordRepository simulationRepository,
            UserService userService,
            ObjectMapper objectMapper) {
        this.simulateUseCase = simulateUseCase;
        this.analyzeGoalUseCase = analyzeGoalUseCase;
        this.reportUseCase = reportUseCase;
        this.diagnosticRepository = diagnosticRepository;
        this.simulationRepository = simulationRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simular cenários financeiros",
               description = "Processa transações do usuário e retorna diagnóstico com cenários RED/YELLOW/GREEN. Persiste resultado.")
    public ResponseEntity<ApiResponse<FinancialAnalysisResponse>> simulate(
            @Valid @RequestBody AnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        SimulationResult result = simulateUseCase.execute(userId, request.getTransactions(), request.getGoal());

        FinancialAnalysisResponse data = new FinancialAnalysisResponse(result.diagnostic(), result.scenarios());
        return ResponseEntity.ok(ApiResponse.success("Simulação processada com sucesso.", data));
    }

    @PostMapping("/report")
    @Operation(summary = "Gerar relatório periódico com comparação de 6 períodos",
               description = "Gera relatório baseado nas transações do banco (WEEKLY/BIWEEKLY/MONTHLY). Persiste resultado.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReport(
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        Map<String, Object> reportData = reportUseCase.generateAndPersistReport(email, request.interval());
        return ResponseEntity.ok(ApiResponse.success("Relatório gerado com sucesso.", reportData));
    }

    @GetMapping("/report/periods")
    @Operation(summary = "Listar períodos disponíveis")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableReportPeriods(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<String> periods = Arrays.stream(ReportInterval.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.success("Períodos de relatório disponíveis", periods));
    }

    // --- SIMULAÇÕES ---

    @GetMapping("/simulation/list")
    @Operation(summary = "Listar simulações do usuário",
               description = "Retorna resumo das simulações realizadas (id, data). Use GET /api/analysis/simulation/{id} para detalhes.")
    public ResponseEntity<ApiResponse<List<SimulationSummaryDto>>> listSimulations(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userService.getEntityByEmail(userDetails.getUsername()).getId();
        List<SimulationRecord> records = simulationRepository.findActiveByUserId(userId);

        if (records.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Não foram encontrados dados.", null));
        }

        List<SimulationSummaryDto> summaries = records.stream()
                .map(r -> new SimulationSummaryDto(r.getId(), r.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                summaries.size() + " simulação(ões) encontrada(s).", summaries));
    }

    @GetMapping("/simulation/{id}")
    @Operation(summary = "Obter simulação por ID")
    public ResponseEntity<ApiResponse<Object>> getSimulation(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userService.getEntityByEmail(userDetails.getUsername()).getId();

        return simulationRepository.findActiveByIdAndUserId(id, userId)
                .map(record -> {
                    try {
                        Map<String, Object> data = Map.of(
                                "id", record.getId(),
                                "goal", objectMapper.readValue(record.getGoalData(), Object.class),
                                "scenarios", objectMapper.readValue(record.getScenarioResults(), Object.class),
                                "diagnostic", objectMapper.readValue(record.getDiagnosticData(), Object.class),
                                "createdAt", record.getCreatedAt()
                        );
                        return ResponseEntity.ok(ApiResponse.success("Simulação encontrada.", (Object) data));
                    } catch (Exception e) {
                        return ResponseEntity.<ApiResponse<Object>>ok(
                                ApiResponse.success("Simulação encontrada.", (Object) record.getScenarioResults()));
                    }
                })
                .orElse(ResponseEntity.ok(ApiResponse.success("Não foram encontrados dados.", null)));
    }

    // --- DIAGNÓSTICOS ---

    @DeleteMapping("/{diagnosticId}")
    @Operation(summary = "Deletar diagnóstico (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteDiagnostic(
            @PathVariable String diagnosticId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        try {
            Diagnostic diagnostic = diagnosticRepository
                    .findByIdAndUserIdAndNotDeleted(diagnosticId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Relatório não encontrado ou não autorizado."));

            diagnostic.setDeletedAt(LocalDateTime.now());
            diagnosticRepository.save(diagnostic);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- CONFIGURAÇÕES ---

    @GetMapping("/config/weights")
    @Operation(summary = "Obter pesos de prioridade")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getWeights(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success("Pesos de prioridade atuais",
            Map.of("ESSENTIAL", 10, "IMPORTANT", 5, "OPTIONAL", 1)));
    }

    @GetMapping("/config/tax-rules")
    @Operation(summary = "Obter regras de tributação")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getTaxRules(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success("Regras de tributação simuladas",
            Map.of("IOF", 0.0038, "IR_SHORT_TERM", 0.225)));
    }

    @GetMapping("/status")
    @Operation(summary = "Status operacional do motor")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success("Status do Motor", "OPERATIONAL"));
    }
}
