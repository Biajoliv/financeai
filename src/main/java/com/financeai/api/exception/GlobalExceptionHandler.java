package com.financeai.api.exception;

import com.financeai.api.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Erros de autenticação (credenciais inválidas, usuário não encontrado)
     * Permitir que exceções de segurança sejam propagadas, não supprimidas
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Falha de autenticação: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Email ou senha inválidos."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Argumento inválido: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Requisição inválida."));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointer(NullPointerException ex) {
        logger.error("NullPointerException processada", ex);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Erro ao processar requisição: certifique-se de enviar todos os campos obrigatórios."));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFormat(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        logger.error("HttpMessageNotReadableException: formato inválido", ex);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Erro ao processar dados enviados. Verifique o formato."));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        logger.error("Erro de validação: {}", ex.getBindingResult().getFieldError().getDefaultMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("Erro de validação em um ou mais campos enviados."));
    }

    /**
     * Catch-all para exceções não tratadas — ÚLTIMO a ser verificado
     * Não deve pegar AuthenticationException ou IllegalArgumentException
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        logger.error("Exceção não tratada", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Erro ao processar requisição. Tente novamente mais tarde."));
    }
}