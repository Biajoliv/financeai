package com.financeai.api.controller;

import com.financeai.api.dto.ApiResponse;
import com.financeai.api.dto.ReportSummaryDto;
import com.financeai.application.service.UserService;
import com.financeai.domain.entity.ReportRecord;
import com.financeai.infrastructure.persistence.ReportRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/report")
@Tag(name = "Relatórios", description = "Listagem e recuperação de relatórios financeiros gerados")
public class ReportController {

    private final ReportRecordRepository reportRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public ReportController(ReportRecordRepository reportRepository,
                            UserService userService,
                            ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/list")
    @Operation(summary = "Listar relatórios do usuário",
               description = "Retorna resumo dos relatórios gerados (id, tipo, data). Use GET /api/report/{id} para o conteúdo completo.")
    public ResponseEntity<ApiResponse<List<ReportSummaryDto>>> listReports(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String interval) {

        UUID userId = userService.getEntityByEmail(userDetails.getUsername()).getId();

        List<ReportRecord> records = (interval != null)
                ? reportRepository.findActiveByUserIdAndInterval(userId, interval.toUpperCase())
                : reportRepository.findActiveByUserId(userId);

        if (records.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Não foram encontrados dados.", null));
        }

        List<ReportSummaryDto> summaries = records.stream()
                .map(r -> new ReportSummaryDto(r.getId(), r.getIntervalType(), r.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(summaries.size() + " relatório(s) encontrado(s).", summaries));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter relatório por ID",
               description = "Retorna o conteúdo completo de um relatório gerado anteriormente.")
    public ResponseEntity<ApiResponse<Object>> getReport(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userService.getEntityByEmail(userDetails.getUsername()).getId();

        return reportRepository.findActiveByIdAndUserId(id, userId)
                .map(record -> {
                    try {
                        // Reconstrói o objeto do JSON armazenado
                        Object header = objectMapper.readValue(record.getHeaderData(), Object.class);
                        Object reportData = objectMapper.readValue(record.getReportData(), Object.class);
                        Map<String, Object> full = Map.of(
                                "header", header,
                                "report", reportData,
                                "interval", record.getIntervalType(),
                                "createdAt", record.getCreatedAt()
                        );
                        return ResponseEntity.ok(ApiResponse.success("Relatório encontrado.", (Object) full));
                    } catch (Exception e) {
                        return ResponseEntity.<ApiResponse<Object>>ok(
                                ApiResponse.success("Relatório encontrado.", (Object) record.getReportData()));
                    }
                })
                .orElse(ResponseEntity.ok(ApiResponse.success("Não foram encontrados dados.", null)));
    }
}
