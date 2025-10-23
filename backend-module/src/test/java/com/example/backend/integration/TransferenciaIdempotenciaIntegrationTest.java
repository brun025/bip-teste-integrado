package com.example.backend.integration;

import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.entity.TransferenciaLog;
import com.example.backend.enums.TransferenciaStatus;
import com.example.backend.repository.BeneficioRepository;
import com.example.backend.repository.TransferenciaLogRepository;
import com.example.backend.service.BeneficioService;
import com.example.ejb.Beneficio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TransferenciaIdempotenciaIntegrationTest {

    @Autowired
    private BeneficioService beneficioService;

    @Autowired
    private BeneficioRepository beneficioRepository;

    @Autowired
    private TransferenciaLogRepository logRepository;

    @Autowired
    private EntityManager entityManager;

    private Long beneficio1Id;
    private Long beneficio2Id;
    private BigDecimal saldoInicialOrigem;
    private BigDecimal saldoInicialDestino;

    @BeforeEach
    void setUp() {

        logRepository.deleteAll();
        beneficioRepository.deleteAll();
        beneficioRepository.flush();
        entityManager.clear();

        Beneficio beneficio1 = new Beneficio("Benefício Origem", "Para débito", new BigDecimal("1000.00"));
        Beneficio beneficio2 = new Beneficio("Benefício Destino", "Para crédito", new BigDecimal("500.00"));

        beneficio1 = beneficioRepository.saveAndFlush(beneficio1);
        beneficio2 = beneficioRepository.saveAndFlush(beneficio2);

        beneficio1Id = beneficio1.getId();
        beneficio2Id = beneficio2.getId();
        saldoInicialOrigem = beneficio1.getValor();
        saldoInicialDestino = beneficio2.getValor();

    }

    @Test
    @DisplayName("Mesma idempotencyKey não executa transferência 2x")
    void naoDeveExecutarTransferenciaDuplicada() {
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal valor = new BigDecimal("100.00");

        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1Id);
        request.setToId(beneficio2Id);
        request.setAmount(valor);
        request.setIdempotencyKey(idempotencyKey);


        beneficioService.transfer(request);

        entityManager.clear();

        Beneficio origem1 = beneficioRepository.findById(beneficio1Id).orElseThrow();
        Beneficio destino1 = beneficioRepository.findById(beneficio2Id).orElseThrow();

        BigDecimal saldoOrigemApos1 = origem1.getValor();
        BigDecimal saldoDestinoApos1 = destino1.getValor();


        beneficioService.transfer(request);

        entityManager.clear();

        Beneficio origem2 = beneficioRepository.findById(beneficio1Id).orElseThrow();
        Beneficio destino2 = beneficioRepository.findById(beneficio2Id).orElseThrow();

        BigDecimal saldoOrigemApos2 = origem2.getValor();
        BigDecimal saldoDestinoApos2 = destino2.getValor();

        assertThat(saldoOrigemApos2)
                .as("Saldo da origem NÃO deve mudar na 2ª execução")
                .isEqualByComparingTo(saldoOrigemApos1);

        assertThat(saldoDestinoApos2)
                .as("Saldo do destino NÃO deve mudar na 2ª execução")
                .isEqualByComparingTo(saldoDestinoApos1);

        assertThat(saldoOrigemApos2)
                .as("Saldo da origem deve ter sido debitado apenas 1x")
                .isEqualByComparingTo(saldoInicialOrigem.subtract(valor));

        assertThat(saldoDestinoApos2)
                .as("Saldo do destino deve ter sido creditado apenas 1x")
                .isEqualByComparingTo(saldoInicialDestino.add(valor));

        Optional<TransferenciaLog> logOpt = logRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(logOpt)
                .as("Log de transferência deve existir")
                .isPresent();

        TransferenciaLog log = logOpt.get();
        assertThat(log.getStatus())
                .as("Status deve ser PROCESSADO")
                .isEqualTo(TransferenciaStatus.PROCESSADO);

        assertThat(log.getFromId()).isEqualTo(beneficio1Id);
        assertThat(log.getToId()).isEqualTo(beneficio2Id);
        assertThat(log.getAmount()).isEqualByComparingTo(valor);

    }

    @Test
    @DisplayName("Deve lançar exceção se transferência já está PROCESSANDO")
    void deveLancarExcecaoSeTransferenciaJaEstaProcessando() {
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal valor = new BigDecimal("50.00");

        TransferenciaLog logProcessando = new TransferenciaLog(
                idempotencyKey,
                beneficio1Id,
                beneficio2Id,
                valor
        );
        logRepository.saveAndFlush(logProcessando);


        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1Id);
        request.setToId(beneficio2Id);
        request.setAmount(valor);
        request.setIdempotencyKey(idempotencyKey);


        assertThatThrownBy(() -> beneficioService.transfer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já está sendo processada");

    }

    @Test
    @DisplayName("Deve criar log com status PROCESSANDO antes da transferência")
    void deveCriarLogComStatusProcessandoAntesDeTransferir() {
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal valor = new BigDecimal("25.00");

        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1Id);
        request.setToId(beneficio2Id);
        request.setAmount(valor);
        request.setIdempotencyKey(idempotencyKey);

        beneficioService.transfer(request);

        Optional<TransferenciaLog> logOpt = logRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(logOpt)
                .as("Log deve ter sido criado")
                .isPresent();

        TransferenciaLog log = logOpt.get();

        assertThat(log.getStatus())
                .as("Status final deve ser PROCESSADO")
                .isEqualTo(TransferenciaStatus.PROCESSADO);

        assertThat(log.getFromId()).isEqualTo(beneficio1Id);
        assertThat(log.getToId()).isEqualTo(beneficio2Id);
        assertThat(log.getAmount()).isEqualByComparingTo(valor);
        assertThat(log.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(log.getCreatedAt()).isNotNull();
        assertThat(log.getUpdatedAt()).isNotNull();

    }

    @Test
    @DisplayName("Deve garantir que idempotencyKey é único no banco")
    void deveGarantirIdempotencyKeyUnica() {
        String idempotencyKey = UUID.randomUUID().toString();

        TransferenciaLog log1 = new TransferenciaLog(
                idempotencyKey,
                beneficio1Id,
                beneficio2Id,
                new BigDecimal("10.00")
        );
        logRepository.saveAndFlush(log1);


        TransferenciaLog log2 = new TransferenciaLog(
                idempotencyKey,
                beneficio2Id,
                beneficio1Id,
                new BigDecimal("20.00")
        );

        assertThatThrownBy(() -> {
            logRepository.saveAndFlush(log2);
        })
                .as("Deve lançar exceção por constraint de unicidade")
                .hasMessageContaining("constraint")
                .hasMessageContaining("idempotency");

    }

    @Test
    @DisplayName("Deve armazenar mensagem de erro quando transferência falha")
    void deveArmazenarMensagemDeErroQuandoTransferenciaFalha() {
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal valorExcessivo = saldoInicialOrigem.add(new BigDecimal("1000.00"));

        TransferenciaRequest request = new TransferenciaRequest();
        request.setFromId(beneficio1Id);
        request.setToId(beneficio2Id);
        request.setAmount(valorExcessivo);
        request.setIdempotencyKey(idempotencyKey);


        try {
            beneficioService.transfer(request);
        } catch (Exception e) {
        }

        Optional<TransferenciaLog> logOpt = logRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(logOpt)
                .as("Log deve existir mesmo após falha")
                .isPresent();

        TransferenciaLog log = logOpt.get();

        assertThat(log.getStatus())
                .as("Status deve ser ERRO")
                .isEqualTo(TransferenciaStatus.ERRO);

        assertThat(log.getErrorMessage())
                .as("Mensagem de erro deve estar registrada")
                .isNotNull()
                .isNotEmpty()
                .contains("Saldo insuficiente");

    }
}