package com.graduacionesisamar.controlescolar.credential.repository;

import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Provides database operations for student credentials.
 */
public interface CredentialRepository
        extends JpaRepository<Credential, Long> {

    boolean existsByStudent_IdAndActiveTrue(Long studentId);

    Optional<Credential> findByStudent_IdAndActiveTrue(Long studentId);

    Optional<Credential> findByQrTokenAndActiveTrue(String qrToken);
}