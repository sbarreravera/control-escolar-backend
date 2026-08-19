package com.graduacionesisamar.controlescolar.guardiandevice.dto;

import java.time.OffsetDateTime;

/**
 * Represents a guardian device returned by the API.
 */
public record GuardianDeviceResponse(

        Long id,

        Long guardianId,

        String deviceName,

        Boolean active,

        OffsetDateTime registeredAt,

        OffsetDateTime lastUsedAt

) {
}