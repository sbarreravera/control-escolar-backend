package com.graduacionesisamar.controlescolar.guardianportal.dto;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;

import java.time.OffsetDateTime;

/**
 * One entry or exit event visible in the guardian portal.
 */
public record GuardianAccessEventResponse(
        Long id,
        Long studentId,
        String studentName,
        String enrollmentNumber,
        AccessEventType eventType,
        OffsetDateTime occurredAt
) {
}
