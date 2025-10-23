package com.example.backend.repository;

import com.example.backend.entity.TransferenciaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferenciaLogRepository extends JpaRepository<TransferenciaLog, Long> {

    Optional<TransferenciaLog> findByIdempotencyKey(String idempotencyKey);

}