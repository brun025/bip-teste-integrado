package com.example.backend.service;

import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.entity.TransferenciaLog;
import com.example.backend.enums.TransferenciaStatus;
import com.example.backend.integration.BeneficioEjbClient;
import com.example.backend.repository.TransferenciaLogRepository;
import com.example.backend.service.impl.TransferenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock
    private BeneficioEjbClient ejbClient;

    @Mock
    private TransferenciaLogRepository logRepository;

    @InjectMocks
    private TransferenciaServiceImpl service;

    private TransferenciaRequest request;
    private TransferenciaLog log;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();

        request = new TransferenciaRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey(idempotencyKey);

        log = new TransferenciaLog(
                idempotencyKey,
                1L,
                2L,
                new BigDecimal("100.00")
        );
        log.setId(1L);
    }


    @Test
    @DisplayName("Deve executar transferência pela primeira vez")
    void deveExecutarTransferenciaPelaPrimeiraVez() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        service.executarTransferencia(request);

        verify(logRepository).findByIdempotencyKey(idempotencyKey);
        verify(logRepository, times(2)).save(any(TransferenciaLog.class));
        verify(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Deve criar log com status PROCESSANDO antes de executar")
    void deveCriarLogComStatusProcessandoAntesDeExecutar() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        service.executarTransferencia(request);

        verify(logRepository).save(argThat(l ->
                l.getIdempotencyKey().equals(idempotencyKey) &&
                        l.getStatus() == TransferenciaStatus.PROCESSANDO
        ));
    }


    @Test
    @DisplayName("Não deve reexecutar se já foi processada (status PROCESSADO)")
    void naoDeveReexecutarSeJaFoiProcessada() {
        log.marcarComoProcessado();
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(log));

        service.executarTransferencia(request);

        verify(logRepository).findByIdempotencyKey(idempotencyKey);
        verify(ejbClient, never()).transfer(any(), any(), any());
        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção se transferência já está PROCESSANDO")
    void deveLancarExcecaoSeTransferenciaJaEstaProcessando() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.executarTransferencia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já está sendo processada");

        verify(logRepository).findByIdempotencyKey(idempotencyKey);
        verify(ejbClient, never()).transfer(any(), any(), any());
    }

    @Test
    @DisplayName("Deve permitir retry se transferência anterior falhou (status ERRO)")
    void devePermitirRetrySeTransferenciaAnteriorFalhou() {
        log.marcarComoErro("Erro anterior");
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(log));
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        service.executarTransferencia(request);

        verify(logRepository).findByIdempotencyKey(idempotencyKey);
        verify(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));
        verify(logRepository, times(2)).save(any(TransferenciaLog.class));
    }

    @Test
    @DisplayName("Deve atualizar log para PROCESSADO após sucesso")
    void deveAtualizarLogParaProcessadoAposSucesso() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        service.executarTransferencia(request);

        verify(logRepository, times(2)).save(any(TransferenciaLog.class));
        verify(logRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve armazenar mensagem de erro no log quando falhar")
    void deveArmazenarMensagemDeErroNoLogQuandoFalhar() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        String mensagemErro = "Benefício origem inativo";
        doThrow(new IllegalStateException(mensagemErro))
                .when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        try {
            service.executarTransferencia(request);
        } catch (Exception e) {
        }

        verify(logRepository).save(argThat(l ->
                l.getStatus() == TransferenciaStatus.ERRO &&
                        l.getErrorMessage() != null &&
                        l.getErrorMessage().contains(mensagemErro)
        ));
    }


    @Test
    @DisplayName("Deve chamar EjbClient com parâmetros corretos")
    void deveChamarEjbClientComParametrosCorretos() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        service.executarTransferencia(request);

        verify(ejbClient).transfer(
                eq(1L),
                eq(2L),
                argThat(amount -> amount.compareTo(new BigDecimal("100.00")) == 0)
        );
    }

    @Test
    @DisplayName("Deve propagar exceção do EjbClient")
    void devePropagarExcecaoDoEjbClient() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        RuntimeException excecao = new RuntimeException("Erro do EJB");
        doThrow(excecao).when(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.executarTransferencia(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro do EJB");

        verify(ejbClient).transfer(1L, 2L, new BigDecimal("100.00"));
    }


    @Test
    @DisplayName("Deve executar com diferentes valores")
    void deveExecutarComDiferentesValores() {
        request.setAmount(new BigDecimal("999.99"));

        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(any(), any(), any());

        service.executarTransferencia(request);

        verify(ejbClient).transfer(
                any(),
                any(),
                argThat(amount -> amount.compareTo(new BigDecimal("999.99")) == 0)
        );
    }

    @Test
    @DisplayName("Deve executar com diferentes IDs de benefícios")
    void deveExecutarComDiferentesIdsDeBeneficios() {
        request.setFromId(999L);
        request.setToId(888L);

        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(ejbClient).transfer(any(), any(), any());

        service.executarTransferencia(request);

        verify(ejbClient).transfer(eq(999L), eq(888L), any());
    }


    @Test
    @DisplayName("Deve criar log em transação independente (REQUIRES_NEW)")
    void deveCriarLogEmTransacaoIndependente() {
        when(logRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(logRepository.save(any(TransferenciaLog.class))).thenReturn(log);
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        doThrow(new RuntimeException("Falha no EJB"))
                .when(ejbClient).transfer(any(), any(), any());

        try {
            service.executarTransferencia(request);
        } catch (Exception e) {
        }

        verify(logRepository).save(argThat(l ->
                l.getStatus() == TransferenciaStatus.PROCESSANDO
        ));

        verify(logRepository).save(argThat(l ->
                l.getStatus() == TransferenciaStatus.ERRO
        ));
    }
}