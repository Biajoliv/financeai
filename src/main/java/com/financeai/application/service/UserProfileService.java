package com.financeai.application.service;

import com.financeai.api.dto.UpdateProfileRequest;
import com.financeai.api.dto.UserProfileDto;
import com.financeai.domain.entity.User;
import com.financeai.domain.entity.UserProfile;
import com.financeai.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);
    private static final Set<String> VALID_INTERVALS = Set.of("WEEKLY", "BIWEEKLY", "MONTHLY");

    private final UserProfileRepository profileRepository;
    private final UserService userService;

    public UserProfileService(UserProfileRepository profileRepository, UserService userService) {
        this.profileRepository = profileRepository;
        this.userService = userService;
    }

    /**
     * Retorna o perfil do usuário. Se não existir, cria um perfil padrão (lazy init).
     * Evita modificar AuthUseCase — o perfil é criado no primeiro acesso.
     */
    @Transactional
    public UserProfileDto getOrCreateProfile(String email) {
        User user = userService.getEntityByEmail(email);
        UUID userId = user.getId();

        return profileRepository.findActiveByUserId(userId)
                .map(UserProfileDto::from)
                .orElseGet(() -> {
                    // Cria perfil padrão na primeira consulta
                    UserProfile defaultProfile = UserProfile.defaultProfile(userId, user.getName());
                    profileRepository.save(defaultProfile);
                    logger.info("[Tenant: {}] Perfil padrão criado automaticamente", email);
                    return UserProfileDto.from(defaultProfile);
                });
    }

    /**
     * Atualiza APENAS campos mutáveis. Campos imutáveis lançam exceção com mensagem amigável.
     */
    @Transactional
    public UserProfileDto updateProfile(String email, UpdateProfileRequest request) {
        // Validação de campos imutáveis
        validateImmutableFields(request);

        // Validação de report_preference
        if (request.reportPreference() != null && !VALID_INTERVALS.contains(request.reportPreference())) {
            throw new IllegalArgumentException("report_preference deve ser WEEKLY, BIWEEKLY ou MONTHLY.");
        }

        User user = userService.getEntityByEmail(email);
        UUID userId = user.getId();

        UserProfile profile = profileRepository.findActiveByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = UserProfile.defaultProfile(userId, user.getName());
                    return profileRepository.save(p);
                });

        // Aplica apenas campos fornecidos (patch parcial)
        if (request.name() != null) profile.setName(request.name());
        if (request.hasOpenFinanceActive() != null) profile.setHasOpenFinanceActive(request.hasOpenFinanceActive());
        if (request.reportPreference() != null) profile.setReportPreference(request.reportPreference());
        if (request.postalCode() != null) profile.setPostalCode(request.postalCode());

        return UserProfileDto.from(profileRepository.save(profile));
    }

    private void validateImmutableFields(UpdateProfileRequest request) {
        if (request.email() != null) {
            throw new IllegalArgumentException("E-mail não pode ser alterado.");
        }
        if (request.password() != null) {
            throw new IllegalArgumentException("Senha não pode ser alterada.");
        }
        if (request.cpfCnpj() != null) {
            throw new IllegalArgumentException("Documento não pode ser alterado.");
        }
        if (request.holderType() != null) {
            throw new IllegalArgumentException("Tipo de titular não pode ser alterado.");
        }
    }

    public UserProfile getProfileEntity(UUID userId) {
        return profileRepository.findActiveByUserId(userId)
                .orElse(null);
    }
}
