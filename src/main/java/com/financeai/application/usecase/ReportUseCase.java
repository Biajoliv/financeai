package com.financeai.application.usecase;

import com.financeai.application.service.RecommendationService;
import com.financeai.application.service.UserProfileService;
import com.financeai.application.service.UserService;
import com.financeai.domain.entity.ReportRecord;
import com.financeai.domain.entity.UserProfile;
import com.financeai.domain.model.ReportInterval;
import com.financeai.domain.model.Transaction;
import com.financeai.domain.service.TransactionProvider;
import com.financeai.infrastructure.persistence.ReportRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReportUseCase.class);

    private final TransactionProvider transactionProvider;
    private final RecommendationService recommendationService;
    private final UserService userService;
    private final UserProfileService profileService;
    private final ReportRecordRepository reportRepository;
    private final ObjectMapper objectMapper;

    public ReportUseCase(
            TransactionProvider transactionProvider,
            RecommendationService recommendationService,
            UserService userService,
            UserProfileService profileService,
            ReportRecordRepository reportRepository,
            ObjectMapper objectMapper) {
        this.transactionProvider = transactionProvider;
        this.recommendationService = recommendationService;
        this.userService = userService;
        this.profileService = profileService;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Gera relatório periódico com comparação de 6 períodos e persiste no banco.
     * Intervalo padrão vem do perfil do usuário; pode ser sobrescrito por parâmetro.
     */
    @Transactional
    public Map<String, Object> generateAndPersistReport(String email, ReportInterval intervalOverride) {
        var user = userService.getEntityByEmail(email);
        UUID userId = user.getId();

        // Determina intervalo: usa o do parâmetro ou o do perfil do usuário
        UserProfile profile = profileService.getProfileEntity(userId);
        ReportInterval interval = intervalOverride != null
                ? intervalOverride
                : resolveProfileInterval(profile);

        // Carrega transações dos últimos 6 períodos completos
        int daysPerPeriod = interval.getDays();
        LocalDate today = LocalDate.now();

        // Agrupa transações por período para montar a comparação
        Map<String, List<BigDecimal>> comparisonByCategory = new LinkedHashMap<>();
        BigDecimal reportBalance = BigDecimal.ZERO;
        List<String> alerts = new ArrayList<>();

        for (int periodIndex = 5; periodIndex >= 0; periodIndex--) {
            LocalDate periodEnd   = today.minusDays((long) periodIndex * daysPerPeriod);
            LocalDate periodStart = periodEnd.minusDays(daysPerPeriod - 1);

            // Usa o período mais recente como saldo do relatório
            List<Transaction> periodTransactions = getPeriodTransactions(email, periodStart, periodEnd);

            if (periodIndex == 0) {
                reportBalance = calculateBalance(periodTransactions);
                alerts = generateAlerts(periodTransactions, reportBalance);
            }

            // Soma gastos por categoria no período
            Map<String, BigDecimal> expensesByCategory = periodTransactions.stream()
                    .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0)
                    .collect(Collectors.groupingBy(
                            Transaction::category,
                            Collectors.reducing(BigDecimal.ZERO,
                                    t -> t.amount().abs(), BigDecimal::add)));

            // periodIndex deve ser efetivamente final para uso em lambda
            final int finalPeriodIndex = periodIndex;
            expensesByCategory.forEach((category, total) ->
                    comparisonByCategory
                            .computeIfAbsent(category, k -> new ArrayList<>(Collections.nCopies(6, BigDecimal.ZERO)))
                            .set(5 - finalPeriodIndex, total));
        }

        // Monta estrutura de resposta
        String userName = (profile != null && profile.getName() != null && !profile.getName().isBlank())
                ? profile.getName()
                : user.getName() != null ? user.getName() : "Usuário";

        String periodLabel = buildPeriodLabel(interval, today);

        Map<String, Object> header = Map.of(
                "title", "Relatório " + translateInterval(interval),
                "subtitle", "Período: " + periodLabel,
                "name", userName
        );

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportBalance", reportBalance);
        report.put("comparison", comparisonByCategory);
        report.put("alerts", alerts);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("header", header);
        result.put("report", report);

        // Persiste o relatório no banco
        persistReport(userId, interval, header, report);

        logger.info("[Tenant: {}] Relatório {} gerado e persistido com {} categorias",
                email, interval.name(), comparisonByCategory.size());

        return result;
    }

    private List<Transaction> getPeriodTransactions(String email, LocalDate start, LocalDate end) {
        // Filtra as transações do provedor pelo intervalo de datas
        return transactionProvider.getTransactionsByUserId(email)
                .stream()
                .filter(t -> !t.date().isBefore(start) && !t.date().isAfter(end))
                .toList();
    }

    private BigDecimal calculateBalance(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> generateAlerts(List<Transaction> transactions, BigDecimal balance) {
        List<String> alerts = new ArrayList<>();

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            alerts.add("Saldo negativo detectado no período atual.");
        }

        BigDecimal essentialExpenses = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) < 0 && t.isEssential())
                .map(t -> t.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal income = transactions.stream()
                .filter(t -> t.amount().compareTo(BigDecimal.ZERO) > 0)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = essentialExpenses.divide(income, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.80")) > 0) {
                alerts.add("Risco de liquidez detectado no curto prazo.");
            }
            if (ratio.compareTo(new BigDecimal("0.60")) > 0) {
                alerts.add("Gastos essenciais representam mais de 60% da renda.");
            }
        }

        if (alerts.isEmpty()) {
            alerts.add("Situação financeira estável no período.");
        }

        return alerts;
    }

    private void persistReport(UUID userId, ReportInterval interval,
                               Map<String, Object> header, Map<String, Object> report) {
        try {
            ReportRecord record = ReportRecord.builder()
                    .userId(userId)
                    .intervalType(interval.name())
                    .headerData(objectMapper.writeValueAsString(header))
                    .reportData(objectMapper.writeValueAsString(report))
                    .build();
            reportRepository.save(record);
        } catch (Exception e) {
            logger.error("Erro ao persistir relatório: {}", e.getMessage());
        }
    }

    private ReportInterval resolveProfileInterval(UserProfile profile) {
        if (profile == null) return ReportInterval.MONTHLY;
        return switch (profile.getReportPreference()) {
            case "WEEKLY"    -> ReportInterval.WEEKLY;
            case "BIWEEKLY"  -> ReportInterval.BIWEEKLY;
            default          -> ReportInterval.MONTHLY;
        };
    }

    private String translateInterval(ReportInterval interval) {
        return switch (interval) {
            case WEEKLY    -> "Semanal";
            case BIWEEKLY  -> "Quinzenal";
            case MONTHLY   -> "Mensal";
        };
    }

    private String buildPeriodLabel(ReportInterval interval, LocalDate today) {
        Locale ptBr = Locale.of("pt", "BR");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", ptBr);
        LocalDate sixPeriodsAgo = today.minusDays((long) 6 * interval.getDays());
        return capitalize(sixPeriodsAgo.format(monthFmt)) + " – " + capitalize(today.format(monthFmt));
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
