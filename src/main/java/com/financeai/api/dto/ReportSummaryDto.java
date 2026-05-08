package com.financeai.api.dto;

import java.time.LocalDateTime;

public record ReportSummaryDto(String id, String intervalType, LocalDateTime createdAt) {}
