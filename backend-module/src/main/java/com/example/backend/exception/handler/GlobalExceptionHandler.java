package com.example.backend.exception.handler;

import com.example.backend.dto.response.ErrorResponse;
import com.example.ejb.exception.*;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Benefício não encontrado (404)
     */
    @ExceptionHandler(BeneficioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBeneficioNotFound(
            BeneficioNotFoundException ex, WebRequest request) {

        logger.error("Benefício não encontrado: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Benefício não encontrado",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Saldo insuficiente (422)
     */
    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleSaldoInsuficiente(
            SaldoInsuficienteException ex, WebRequest request) {

        logger.error("Saldo insuficiente: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Saldo insuficiente",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Transferência inválida (400)
     */
    @ExceptionHandler(TransferenciaInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransferenciaInvalida(
            TransferenciaInvalidaException ex, WebRequest request) {

        logger.error("Transferência inválida: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Transferência inválida",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Conflito de concorrência (409)
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException ex, WebRequest request) {

        logger.error("Conflito de concorrência detectado: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflito de concorrência",
                "Outro usuário modificou este registro. Por favor, tente novamente.",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Estado ilegal (409)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {

        logger.error("Estado ilegal: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Operação inválida",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Argumento ilegal (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        logger.error("Argumento ilegal: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Argumento inválido",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Erros de validação (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Erro de validação");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());

        logger.error("Erro de validação: {}", errors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Exceção genérica (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        logger.error("Erro inesperado: ", ex);

        logger.error("Tipo da exceção: {}", ex.getClass().getName());
        logger.error("Mensagem: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno do servidor",
                "Ocorreu um erro inesperado. Por favor, contate o suporte.",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}