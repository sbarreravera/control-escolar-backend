package com.graduacionesisamar.controlescolar.school.dto;

import java.time.OffsetDateTime;

/**
 * Represents school information returned by the API.
 */
public record SchoolResponse(
        Long id,
        String name,
        String code,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}