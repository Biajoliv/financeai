package com.financeai.application.service;

import com.financeai.api.dto.UserDto;
import com.financeai.domain.entity.User;
import com.financeai.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto getByEmail(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        return UserDto.from(user);
    }

    /**
     * PUT /api/user — valida campos imutáveis e aplica apenas o systemRole.
     * email, password, cpfCnpj e holderType NÃO podem ser alterados.
     */
    @Transactional
    public UserDto updateSystemRole(String email, String newRole) {
        if (newRole == null) return getByEmail(email);

        if (!newRole.equals("FREE") && !newRole.equals("PREMIUM")) {
            throw new IllegalArgumentException("system_role deve ser FREE ou PREMIUM.");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        user.setSystemRole(newRole);
        return UserDto.from(userRepository.save(user));
    }

    public User getEntityByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
}
