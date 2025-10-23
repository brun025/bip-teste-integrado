package com.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.example.ejb.exception.*;
import java.math.BigDecimal;

@Stateless
@Component
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class BeneficioEjbService {

    private static final Logger logger = LoggerFactory.getLogger(BeneficioEjbService.class);

    private final EntityManager em;

    public BeneficioEjbService(EntityManager em) {
        this.em = em;
        logger.info("[EJB] BeneficioEjbService inicializado com EntityManager do Spring");
    }

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        logger.info("[EJB] Iniciando transferência: FROM={}, TO={}, AMOUNT={}", fromId, toId, amount);

        validarParametros(fromId, toId, amount);

        if (fromId.equals(toId)) {
            throw new TransferenciaInvalidaException(
                    "Não é possível transferir para o mesmo benefício"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferenciaInvalidaException(
                    String.format("Valor da transferência deve ser positivo: %s", amount)
            );
        }

        try {
            Beneficio from = findBeneficioById(fromId);
            Beneficio to = findBeneficioById(toId);

            logger.debug("[EJB] Benefícios encontrados - FROM: {} (version={}), TO: {} (version={})",
                    from.getNome(), from.getVersion(), to.getNome(), to.getVersion());

            from.validarAtivo();
            to.validarAtivo();

            from.debitar(amount);
            to.creditar(amount);

            em.merge(from);
            em.merge(to);
            em.flush();

            logger.info("[EJB] Transferência concluída com sucesso: FROM={} (saldo={}, version={}), TO={} (saldo={}, version={})",
                    fromId, from.getValor(), from.getVersion(), toId, to.getValor(), to.getVersion());

        } catch (OptimisticLockException e) {
            logger.warn("[EJB] Conflito de concorrência detectado na transferência: FROM={}, TO={}",
                    fromId, toId);
            throw e;

        } catch (BeneficioNotFoundException | SaldoInsuficienteException | IllegalStateException e) {
            logger.error("[EJB] Erro de validação na transferência: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("[EJB] Erro inesperado na transferência: FROM={}, TO={}, AMOUNT={}",
                    fromId, toId, amount, e);
            throw new RuntimeException("Erro ao processar transferência", e);
        }
    }

    public Beneficio findBeneficioById(Long id) {
        logger.debug("[EJB] Buscando benefício ID: {}", id);
        Beneficio beneficio = em.find(Beneficio.class, id);
        if (beneficio == null) {
            logger.error("[EJB] Benefício não encontrado: ID={}", id);
            throw new BeneficioNotFoundException(id);
        }
        logger.debug("[EJB] Benefício encontrado: {}", beneficio);
        return beneficio;
    }

    private void validarParametros(Long fromId, Long toId, BigDecimal amount) {
        if (fromId == null) {
            throw new TransferenciaInvalidaException("ID do benefício origem não pode ser nulo");
        }
        if (toId == null) {
            throw new TransferenciaInvalidaException("ID do benefício destino não pode ser nulo");
        }
        if (amount == null) {
            throw new TransferenciaInvalidaException("Valor da transferência não pode ser nulo");
        }
    }
}