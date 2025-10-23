package com.example.backend.integration;

import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.repository.BeneficioRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static java.time.Duration.ofSeconds;

@SpringBootTest
@ActiveProfiles("test")
class BeneficioConcorrenciaIntegrationTest {

    @Autowired
    private BeneficioService beneficioService;

    @Autowired
    private BeneficioRepository beneficioRepository;

    @Autowired
    private EntityManager entityManager;

    private Long beneficio1Id;
    private Long beneficio2Id;
    private BigDecimal saldoInicialOrigem;
    private BigDecimal saldoInicialDestino;

    @BeforeEach
    void setUp() {

        beneficioRepository.deleteAll();
        beneficioRepository.flush();
        entityManager.clear();

        Beneficio beneficio1 = new Beneficio("Benefício Origem", "Para débito", new BigDecimal("1000.00"));
        Beneficio beneficio2 = new Beneficio("Benefício Destino", "Para crédito", new BigDecimal("500.00"));

        beneficio1 = beneficioRepository.saveAndFlush(beneficio1);
        beneficio2 = beneficioRepository.saveAndFlush(beneficio2);

        assertThat(beneficio1.getVersion())
                .as("Version deve ser inicializado")
                .isNotNull()
                .isEqualTo(0L);

        beneficio1Id = beneficio1.getId();
        beneficio2Id = beneficio2.getId();
        saldoInicialOrigem = beneficio1.getValor();
        saldoInicialDestino = beneficio2.getValor();
    }

    @Test
    @DisplayName("Previne Lost Update com 5 transferências concorrentes")
    void devePrevenirLostUpdateEmTransferenciasConcorrentes() {
        assertTimeoutPreemptively(ofSeconds(15), () -> {

            int numThreads = 5;
            BigDecimal valorPorTransferencia = new BigDecimal("20.00");
            BigDecimal totalTransferido = valorPorTransferencia.multiply(new BigDecimal(numThreads));

            AtomicInteger sucessos = new AtomicInteger(0);
            AtomicInteger erros = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                    final int threadId = i;
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            TransferenciaRequest request = new TransferenciaRequest();
                            request.setFromId(beneficio1Id);
                            request.setToId(beneficio2Id);
                            request.setAmount(valorPorTransferencia);
                            request.setIdempotencyKey(UUID.randomUUID().toString());

                            beneficioService.transfer(request);

                            sucessos.incrementAndGet();

                        } catch (Exception e) {
                            erros.incrementAndGet();
                        }
                    }, executor);

                    futures.add(future);
                }

                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );

                allOf.get(10, TimeUnit.SECONDS);

            } catch (TimeoutException e) {
                throw new AssertionError("Timeout ao executar transferências concorrentes", e);

            } catch (Exception e) {
                throw new AssertionError("Erro ao executar transferências concorrentes", e);

            } finally {
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            }

            assertThat(sucessos.get())
                    .as("Todas as transferências devem ter sucesso (com retry automático)")
                    .isEqualTo(numThreads);

            assertThat(erros.get())
                    .as("Não deve haver erros após retry automático")
                    .isEqualTo(0);

            Thread.sleep(500);

            entityManager.clear();

            Beneficio origem = beneficioRepository.findById(beneficio1Id)
                    .orElseThrow(() -> new AssertionError("Benefício origem não encontrado"));
            Beneficio destino = beneficioRepository.findById(beneficio2Id)
                    .orElseThrow(() -> new AssertionError("Benefício destino não encontrado"));

            BigDecimal saldoEsperadoOrigem = saldoInicialOrigem.subtract(totalTransferido);
            BigDecimal saldoEsperadoDestino = saldoInicialDestino.add(totalTransferido);

            assertThat(origem.getValor())
                    .as("Saldo da origem após transferências")
                    .isEqualByComparingTo(saldoEsperadoOrigem);

            assertThat(destino.getValor())
                    .as("Saldo do destino após transferências")
                    .isEqualByComparingTo(saldoEsperadoDestino);

            assertThat(origem.getVersion())
                    .as("Version da origem deve ter sido incrementado")
                    .isEqualTo((long) numThreads);

            assertThat(destino.getVersion())
                    .as("Version do destino deve ter sido incrementado")
                    .isEqualTo((long) numThreads);

            BigDecimal saldoTotalInicial = saldoInicialOrigem.add(saldoInicialDestino);
            BigDecimal saldoTotalFinal = origem.getValor().add(destino.getValor());

            assertThat(saldoTotalFinal)
                    .as("Saldo total deve ser conservado (sem Lost Update)")
                    .isEqualByComparingTo(saldoTotalInicial);
        });
    }

    @Test
    @DisplayName("Deve executar 10 transferências sequenciais corretamente")
    void deveExecutarTransferenciasSequenciaisCorretamente() {

        int numTransferencias = 10;
        BigDecimal valor = new BigDecimal("10.00");
        BigDecimal totalTransferido = valor.multiply(new BigDecimal(numTransferencias));

        for (int i = 0; i < numTransferencias; i++) {
            TransferenciaRequest request = new TransferenciaRequest();
            request.setFromId(beneficio1Id);
            request.setToId(beneficio2Id);
            request.setAmount(valor);
            request.setIdempotencyKey(UUID.randomUUID().toString());

            beneficioService.transfer(request);
        }

        entityManager.clear();

        Beneficio origem = beneficioRepository.findById(beneficio1Id).orElseThrow();
        Beneficio destino = beneficioRepository.findById(beneficio2Id).orElseThrow();

        BigDecimal saldoEsperadoOrigem = saldoInicialOrigem.subtract(totalTransferido);
        BigDecimal saldoEsperadoDestino = saldoInicialDestino.add(totalTransferido);

        assertThat(origem.getValor()).isEqualByComparingTo(saldoEsperadoOrigem);
        assertThat(destino.getValor()).isEqualByComparingTo(saldoEsperadoDestino);

        assertThat(origem.getVersion()).isEqualTo((long) numTransferencias);
        assertThat(destino.getVersion()).isEqualTo((long) numTransferencias);
    }

    @Test
    @DisplayName("Deve ter boa performance (5 transferências em < 5 segundos)")
    void deveExecutarComBoaPerformance() {
        assertTimeoutPreemptively(ofSeconds(5), () -> {
            int numThreads = 5;
            BigDecimal valor = new BigDecimal("10.00");

            long inicio = System.currentTimeMillis();

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        TransferenciaRequest request = new TransferenciaRequest();
                        request.setFromId(beneficio1Id);
                        request.setToId(beneficio2Id);
                        request.setAmount(valor);
                        request.setIdempotencyKey(UUID.randomUUID().toString());

                        beneficioService.transfer(request);
                    }, executor);

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            } finally {
                executor.shutdown();
                executor.awaitTermination(3, TimeUnit.SECONDS);
            }

            long duracao = System.currentTimeMillis() - inicio;

            assertThat(duracao)
                    .as("Tempo de execução deve ser inferior a 5 segundos")
                    .isLessThan(5000L);
        });
    }
}