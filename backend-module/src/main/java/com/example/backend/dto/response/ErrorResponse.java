package com.example.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Resposta de erro padronizada")
public class ErrorResponse {

    @Schema(description = "Código HTTP do erro", example = "404")
    private int status;

    @Schema(description = "Tipo do erro", example = "Benefício não encontrado")
    private String error;

    @Schema(description = "Mensagem detalhada", example = "Benefício com ID 999 não encontrado")
    private String message;

    @Schema(description = "Timestamp do erro")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponse() {}

    public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}