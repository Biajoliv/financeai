package com.financeai.api.dto;

import com.financeai.domain.entity.UserProfile;

public record UserProfileDto(
    String name,
    String holderType,
    String businessType,
    boolean hasOpenFinanceActive,
    String reportPreference,
    String postalCode
) {
    public static UserProfileDto from(UserProfile profile) {
        return new UserProfileDto(
            profile.getName(),
            profile.getHolderType(),
            profile.getBusinessType(),
            profile.isHasOpenFinanceActive(),
            profile.getReportPreference(),
            profile.getPostalCode()
        );
    }
}
