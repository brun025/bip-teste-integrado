package com.example.backend.integration;

import com.example.ejb.BeneficioEjbService;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BeneficioEjbClient {

    private static final Logger logger = LoggerFactory.getLogger(BeneficioEjbClient.class);

    private final BeneficioEjbService ejbService;

    public BeneficioEjbClient(BeneficioEjbService ejbService) {
        this.ejbService = ejbService;
        logger.info("BeneficioEjbClient inicializado - Integração com EJB Legado ativa");
    }

    @Retryable(
            retryFor = {OptimisticLockException.class, StaleStateException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 500)
    )
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        logger.info("[CLIENT] Chamando EJB Legado: transfer(FROM={}, TO={}, AMOUNT={})",
                fromId, toId, amount);

        try {
            ejbService.transfer(fromId, toId, amount);

            logger.info("[CLIENT] EJB Legado completou transferência com sucesso");

        } catch (OptimisticLockException | StaleStateException e) {
            logger.warn("[CLIENT] Conflito de concorrência detectado - retry automático: {}",
                    e.getClass().getSimpleName());
            throw e;

        } catch (Exception e) {
            logger.error("[CLIENT] Erro não recuperável ao chamar EJB Legado: {}", e.getMessage());
            throw e;
        }
    }
}