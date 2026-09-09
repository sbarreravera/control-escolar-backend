package com.graduacionesisamar.controlescolar.guardian.dto;

import java.time.OffsetDateTime;

/**
 * Represents guardian information returned by the API.
 */
public record GuardianResponse(
        Long id,
        Long schoolId,
        String schoolName,
        String externalReference,
        String fullName,
        String phone,
        String email,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
