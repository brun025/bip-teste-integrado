package com.example.backend.mapper;

import com.example.backend.dto.request.BeneficioCreateRequest;
import com.example.backend.dto.response.BeneficioResponse;
import com.example.ejb.Beneficio;
import org.springframework.stereotype.Component;

@Component
public class BeneficioMapper {

    public Beneficio toEntity(BeneficioCreateRequest request) {
        Beneficio beneficio = new Beneficio();
        beneficio.setNome(request.getNome());
        beneficio.setDescricao(request.getDescricao());
        beneficio.setValor(request.getValor());
        beneficio.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);
        return beneficio;
    }

    public BeneficioResponse toResponse(Beneficio beneficio) {
        BeneficioResponse response = new BeneficioResponse();
        response.setId(beneficio.getId());
        response.setNome(beneficio.getNome());
        response.setDescricao(beneficio.getDescricao());
        response.setValor(beneficio.getValor());
        response.setAtivo(beneficio.getAtivo());
        response.setVersion(beneficio.getVersion());
        return response;
    }
}