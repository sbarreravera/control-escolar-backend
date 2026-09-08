package com.graduacionesisamar.controlescolar.credential.dto;

import java.time.OffsetDateTime;

/**
 * Contains the information returned for a student credential.
 */
public record CredentialResponse(
        Long id,
        Long studentId,
        String qrToken,
        Boolean active,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime deactivatedAt,
        OffsetDateTime createdAt
) {
}