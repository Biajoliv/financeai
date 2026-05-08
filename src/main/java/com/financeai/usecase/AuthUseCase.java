package com.financeai.usecase;

import com.financeai.domain.entity.User;
import com.financeai.domain.entity.UserProfile;
import com.financeai.domain.valueobject.AuthDtos.*;
import com.financeai.infrastructure.persistence.UserProfileRepository;
import com.financeai.infrastructure.persistence.UserRepository;
import com.financeai.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AuthUseCase.class);
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    public AuthUseCase(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authManager) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authManager = authManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            logger.warn("Tentativa de registrar email já existente");
            // Mensagem segura: não expõe o email conforme boas práticas
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        var user = new User(
            request.name(),
            request.email(),
            passwordEncoder.encode(request.password())
        );

        // savedUser garante que o ID gerado pelo banco é retornado
        var savedUser = userRepository.saveAndFlush(user);

        // Cria perfil padrão imediatamente após o registro
        UserProfile defaultProfile = UserProfile.defaultProfile(savedUser.getId(), request.name());
        profileRepository.save(defaultProfile);

        var token = jwtService.generateToken(savedUser.getId().toString(), savedUser.getEmail());

        logger.info("Usuário registrado com sucesso (id={})", savedUser.getId());
        return new AuthResponse(token, savedUser.getId().toString(), savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException ex) {
            logger.warn("Falha de autenticação para email: {}", request.email());
            throw ex;  // Propagar para o GlobalExceptionHandler tratar
        }

        var user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        var token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        logger.info("Login bem-sucedido para: {}", user.getEmail());
        return new AuthResponse(token, user.getId().toString(), user.getEmail());
    }
}
