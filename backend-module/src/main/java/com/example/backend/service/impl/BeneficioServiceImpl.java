package com.example.backend.service.impl;

import com.example.backend.dto.request.BeneficioCreateRequest;
import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.dto.response.BeneficioResponse;
import com.example.ejb.exception.BeneficioNotFoundException;
import com.example.backend.mapper.BeneficioMapper;
import com.example.backend.repository.BeneficioRepository;
import com.example.backend.service.BeneficioService;
import com.example.backend.service.TransferenciaService;
import com.example.ejb.Beneficio;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BeneficioServiceImpl implements BeneficioService {

    private static final Logger logger = LoggerFactory.getLogger(BeneficioServiceImpl.class);

    private final BeneficioRepository repository;
    private final BeneficioMapper mapper;
    private final TransferenciaService transferenciaService;

    public BeneficioServiceImpl(BeneficioRepository repository,
                                BeneficioMapper mapper,
                                TransferenciaService transferenciaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.transferenciaService = transferenciaService;
    }

    @Override
    public List<BeneficioResponse> findAll() {
        logger.debug("Buscando todos os benefícios ativos");
        return repository.findByAtivoTrue()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BeneficioResponse findById(Long id) {
        logger.debug("Buscando benefício por ID: {}", id);
        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new BeneficioNotFoundException(id));
        return mapper.toResponse(beneficio);
    }

    @Override
    @Transactional
    public BeneficioResponse create(BeneficioCreateRequest request) {
        logger.info("Criando novo benefício: {}", request.getNome());
        Beneficio beneficio = mapper.toEntity(request);
        Beneficio saved = repository.save(beneficio);
        logger.info("Benefício criado com sucesso: ID={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class, StaleStateException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 500)
    )
    public BeneficioResponse update(Long id, BeneficioCreateRequest request) {
        logger.info("Atualizando benefício ID: {}", id);

        try {
            Beneficio beneficio = repository.findById(id)
                    .orElseThrow(() -> new BeneficioNotFoundException(id));

            if (!Boolean.TRUE.equals(beneficio.getAtivo()) &&
                    !Boolean.TRUE.equals(request.getAtivo())) {
                throw new IllegalStateException("Não é possível atualizar benefício inativo");
            }

            beneficio.setNome(request.getNome());
            beneficio.setDescricao(request.getDescricao());
            beneficio.setValor(request.getValor());
            if (request.getAtivo() != null) {
                beneficio.setAtivo(request.getAtivo());
            }

            Beneficio updated = repository.save(beneficio);
            logger.info("Benefício atualizado com sucesso: ID={}, VERSION={}",
                    id, updated.getVersion());

            return mapper.toResponse(updated);

        } catch (OptimisticLockException e) {
            logger.warn("Conflito de concorrência no update do benefício ID: {}", id);
            throw e;
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        logger.info("Desativando benefício ID: {}", id);
        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new BeneficioNotFoundException(id));
        beneficio.setAtivo(false);
        repository.save(beneficio);
        logger.info("Benefício desativado com sucesso: ID={}", id);
    }

    @Override
    public void transfer(TransferenciaRequest request) {
        transferenciaService.executarTransferencia(request);
    }
}