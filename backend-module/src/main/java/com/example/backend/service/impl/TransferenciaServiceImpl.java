package com.example.backend.service.impl;

import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.entity.TransferenciaLog;
import com.example.backend.enums.TransferenciaStatus;
import com.example.backend.integration.BeneficioEjbClient;
import com.example.backend.repository.TransferenciaLogRepository;
import com.example.backend.service.TransferenciaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TransferenciaServiceImpl implements TransferenciaService {

    private static final Logger logger = LoggerFactory.getLogger(TransferenciaServiceImpl.class);

    private final BeneficioEjbClient ejbClient;
    private final TransferenciaLogRepository logRepository;

    public TransferenciaServiceImpl(BeneficioEjbClient ejbClient,
                                    TransferenciaLogRepository logRepository) {
        this.ejbClient = ejbClient;
        this.logRepository = logRepository;
    }

    @Override
    public void executarTransferencia(TransferenciaRequest request) {
        String idempotencyKey = request.getIdempotencyKey();

        logger.info("[TRANSFERENCIA] Iniciando: FROM={}, TO={}, AMOUNT={}, KEY={}",
                request.getFromId(), request.getToId(), request.getAmount(), idempotencyKey);

        Optional<TransferenciaLog> existingLog = logRepository.findByIdempotencyKey(idempotencyKey);

        if (existingLog.isPresent()) {
            TransferenciaLog log = existingLog.get();

            if (log.foiProcessadaComSucesso()) {
                logger.info("[TRANSFERENCIA] Já processada anteriormente - KEY={}", idempotencyKey);
                return;
            }

            if (log.getStatus() == TransferenciaStatus.PROCESSANDO) {
                logger.warn("[TRANSFERENCIA] ⚠️ Já está sendo processada por outra thread - KEY={}", idempotencyKey);
                throw new IllegalStateException("Transferência já está sendo processada");
            }

            logger.info("[TRANSFERENCIA] 🔄 Retry de transferência que falhou - KEY={}", idempotencyKey);
        }

        TransferenciaLog log = criarLogTransferencia(request);

        try {
            ejbClient.transfer(
                    request.getFromId(),
                    request.getToId(),
                    request.getAmount()
            );

            atualizarLogSucesso(log.getId());

            logger.info("[TRANSFERENCIA] Concluída com sucesso - KEY={}", idempotencyKey);

        } catch (Exception e) {
            atualizarLogErro(log.getId(), e.getMessage());

            logger.error("[TRANSFERENCIA] Erro - KEY={}: {}", idempotencyKey, e.getMessage());
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected TransferenciaLog criarLogTransferencia(TransferenciaRequest request) {
        TransferenciaLog log = new TransferenciaLog(
                request.getIdempotencyKey(),
                request.getFromId(),
                request.getToId(),
                request.getAmount()
        );
        log = logRepository.save(log);
        logger.debug("[TRANSFERENCIA] Log criado - ID={}", log.getId());
        return log;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void atualizarLogSucesso(Long logId) {
        TransferenciaLog log = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("Log não encontrado: " + logId));
        log.marcarComoProcessado();
        logRepository.save(log);
        logger.debug("[TRANSFERENCIA] Log atualizado para PROCESSADO - ID={}", logId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void atualizarLogErro(Long logId, String mensagemErro) {
        TransferenciaLog log = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("Log não encontrado: " + logId));
        log.marcarComoErro(mensagemErro);
        logRepository.save(log);
        logger.debug("[TRANSFERENCIA] Log atualizado para ERRO - ID={}", logId);
    }
}