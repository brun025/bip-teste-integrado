package com.example.ejb.exception;

public class BeneficioNotFoundException extends RuntimeException {  // ← RuntimeException!

    private final Long beneficioId;

    public BeneficioNotFoundException(Long beneficioId) {
        super(String.format("Benefício com ID %d não encontrado", beneficioId));
        this.beneficioId = beneficioId;
    }

    public BeneficioNotFoundException(String message) {
        super(message);
        this.beneficioId = null;
    }

    public Long getBeneficioId() {
        return beneficioId;
    }
}