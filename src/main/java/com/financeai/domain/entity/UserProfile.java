package com.financeai.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    // Chave primária = user_id (relacionamento 1-1 com User)
    @Id
    @Column(name = "user_id")
    private java.util.UUID userId;

    // Mapeado para user — não usa @GeneratedValue pois o ID vem do User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "full_name_encrypted")
    private String name;

    // PF = Pessoa Física, PJ = Pessoa Jurídica
    @Column(name = "holder_type")
    @Builder.Default
    private String holderType = "PF";

    // Tipo de empresa: NONE, MEI, EI, LTDA, SA
    @Column(name = "business_type", columnDefinition = "company_type")
    @Builder.Default
    private String businessType = "NONE";

    // Se true, o usuário permite transações de outros bancos via Open Finance
    @Column(name = "has_open_finance_active")
    @Builder.Default
    private boolean hasOpenFinanceActive = false;

    // Preferência de relatório: WEEKLY, BIWEEKLY, MONTHLY
    @Column(name = "report_preference")
    @Builder.Default
    private String reportPreference = "MONTHLY";

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Factory method para criar um perfil padrão ao registrar um novo usuário
    public static UserProfile defaultProfile(java.util.UUID userId, String name) {
        return UserProfile.builder()
                .userId(userId)
                .name(name != null ? name : "")
                .holderType("PF")
                .businessType("NONE")
                .hasOpenFinanceActive(false)
                .reportPreference("MONTHLY")
                .build();
    }
}
