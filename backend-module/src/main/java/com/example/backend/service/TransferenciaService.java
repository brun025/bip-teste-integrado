package com.example.backend.service;

import com.example.backend.dto.request.TransferenciaRequest;

public interface TransferenciaService {

    void executarTransferencia(TransferenciaRequest request);

}