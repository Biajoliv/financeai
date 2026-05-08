package com.financeai.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_records",
       indexes = @Index(name = "idx_report_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // WEEKLY, BIWEEKLY ou MONTHLY
    @Column(name = "interval_type", nullable = false)
    private String intervalType;

    // JSON do header (title, subtitle, name)
    @Column(name = "header_data", columnDefinition = "TEXT")
    private String headerData;

    // JSON do relatório (reportBalance, comparison, alerts)
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
