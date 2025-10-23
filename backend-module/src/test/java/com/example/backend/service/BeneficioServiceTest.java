package com.example.backend.service;

import com.example.backend.dto.request.BeneficioCreateRequest;
import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.dto.response.BeneficioResponse;
import com.example.backend.mapper.BeneficioMapper;
import com.example.backend.repository.BeneficioRepository;
import com.example.backend.service.impl.BeneficioServiceImpl;
import com.example.ejb.Beneficio;
import com.example.ejb.exception.BeneficioNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficioServiceTest {

    @Mock
    private BeneficioRepository repository;

    @Mock
    private BeneficioMapper mapper;

    @Mock
    private TransferenciaService transferenciaService;

    @InjectMocks
    private BeneficioServiceImpl service;

    private Beneficio beneficio;
    private BeneficioCreateRequest createRequest;
    private BeneficioResponse response;

    @BeforeEach
    void setUp() {
        beneficio = new Beneficio(
                "Benefício Teste",
                "Descrição teste",
                new BigDecimal("1000.00")
        );
        beneficio.setId(1L);
        beneficio.setVersion(0L);

        createRequest = new BeneficioCreateRequest();
        createRequest.setNome("Benefício Teste");
        createRequest.setDescricao("Descrição teste");
        createRequest.setValor(new BigDecimal("1000.00"));
        createRequest.setAtivo(true);

        response = new BeneficioResponse();
        response.setId(1L);
        response.setNome("Benefício Teste");
        response.setDescricao("Descrição teste");
        response.setValor(new BigDecimal("1000.00"));
        response.setAtivo(true);
    }


    @Test
    @DisplayName("Deve listar todos os benefícios ativos")
    void deveListarTodosBeneficiosAtivos() {
        Beneficio beneficio2 = new Beneficio("Benefício 2", "Desc 2", new BigDecimal("500.00"));
        beneficio2.setId(2L);

        when(repository.findByAtivoTrue()).thenReturn(Arrays.asList(beneficio, beneficio2));
        when(mapper.toResponse(any(Beneficio.class))).thenReturn(response);

        List<BeneficioResponse> resultado = service.findAll();

        assertThat(resultado)
                .hasSize(2)
                .allMatch(r -> r.getAtivo());

        verify(repository).findByAtivoTrue();
        verify(mapper, times(2)).toResponse(any(Beneficio.class));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há benefícios ativos")
    void deveRetornarListaVaziaQuandoNaoHaBeneficiosAtivos() {
        when(repository.findByAtivoTrue()).thenReturn(List.of());

        List<BeneficioResponse> resultado = service.findAll();

        assertThat(resultado).isEmpty();
        verify(repository).findByAtivoTrue();
        verify(mapper, never()).toResponse(any());
    }


    @Test
    @DisplayName("Deve buscar benefício por ID com sucesso")
    void deveBuscarBeneficioPorIdComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(mapper.toResponse(beneficio)).thenReturn(response);

        BeneficioResponse resultado = service.findById(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNome()).isEqualTo("Benefício Teste");

        verify(repository).findById(1L);
        verify(mapper).toResponse(beneficio);
    }

    @Test
    @DisplayName("Deve lançar exceção quando benefício não existe")
    void deveLancarExcecaoQuandoBeneficioNaoExiste() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(BeneficioNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository).findById(999L);
        verify(mapper, never()).toResponse(any());
    }


    @Test
    @DisplayName("Deve criar benefício com sucesso")
    void deveCriarBeneficioComSucesso() {
        when(mapper.toEntity(createRequest)).thenReturn(beneficio);
        when(repository.save(beneficio)).thenReturn(beneficio);
        when(mapper.toResponse(beneficio)).thenReturn(response);

        BeneficioResponse resultado = service.create(createRequest);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Benefício Teste");
        assertThat(resultado.getValor()).isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(mapper).toEntity(createRequest);
        verify(repository).save(beneficio);
        verify(mapper).toResponse(beneficio);
    }


    @Test
    @DisplayName("Deve atualizar benefício com sucesso")
    void deveAtualizarBeneficioComSucesso() {
        BeneficioCreateRequest updateRequest = new BeneficioCreateRequest();
        updateRequest.setNome("Benefício Atualizado");
        updateRequest.setDescricao("Nova descrição");
        updateRequest.setValor(new BigDecimal("1500.00"));
        updateRequest.setAtivo(true);

        when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(repository.save(any(Beneficio.class))).thenReturn(beneficio);
        when(mapper.toResponse(any(Beneficio.class))).thenReturn(response);

        BeneficioResponse resultado = service.update(1L, updateRequest);

        assertThat(resultado).isNotNull();
        verify(repository).findById(1L);
        verify(repository).save(any(Beneficio.class));
        verify(mapper).toResponse(any(Beneficio.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar benefício inexistente")
    void deveLancarExcecaoAoAtualizarBeneficioInexistente() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, createRequest))
                .isInstanceOf(BeneficioNotFoundException.class);

        verify(repository).findById(999L);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve permitir atualizar benefício inativo")
    void naoDevePermitirAtualizarBeneficioInativo() {
        beneficio.setAtivo(false);
        createRequest.setAtivo(false);

        when(repository.findById(1L)).thenReturn(Optional.of(beneficio));

        assertThatThrownBy(() -> service.update(1L, createRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativo");

        verify(repository).findById(1L);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve desativar benefício com sucesso")
    void deveDesativarBeneficioComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(repository.save(any(Beneficio.class))).thenReturn(beneficio);

        service.delete(1L);

        verify(repository).findById(1L);
        verify(repository).save(any(Beneficio.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao desativar benefício inexistente")
    void deveLancarExcecaoAoDesativarBeneficioInexistente() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(BeneficioNotFoundException.class);

        verify(repository).findById(999L);
        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("Deve delegar transferência para TransferenciaService")
    void deveDelegarTransferenciaParaTransferenciaService() {
        TransferenciaRequest request = new TransferenciaRequest(1L, 2L, new BigDecimal("100.00"));
        doNothing().when(transferenciaService).executarTransferencia(request);

        service.transfer(request);

        verify(transferenciaService).executarTransferencia(request);
    }

    @Test
    @DisplayName("Deve propagar exceção de TransferenciaService")
    void devePropagarExcecaoDeTransferenciaService() {
        TransferenciaRequest request = new TransferenciaRequest(1L, 2L, new BigDecimal("100.00"));
        doThrow(new IllegalArgumentException("Erro na transferência"))
                .when(transferenciaService).executarTransferencia(request);

        assertThatThrownBy(() -> service.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Erro na transferência");

        verify(transferenciaService).executarTransferencia(request);
    }
}