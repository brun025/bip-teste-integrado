package com.example.backend.service;

import com.example.backend.dto.request.BeneficioCreateRequest;
import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.dto.response.BeneficioResponse;
import java.util.List;

public interface BeneficioService {

    List<BeneficioResponse> findAll();

    BeneficioResponse findById(Long id);

    BeneficioResponse create(BeneficioCreateRequest request);

    BeneficioResponse update(Long id, BeneficioCreateRequest request);

    void delete(Long id);

    void transfer(TransferenciaRequest request);
}