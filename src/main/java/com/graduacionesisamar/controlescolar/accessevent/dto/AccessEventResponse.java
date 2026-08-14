package com.graduacionesisamar.controlescolar.accessevent.dto;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;

import java.time.OffsetDateTime;

/**
 * Contains the information returned for an access event.
 */
public record AccessEventResponse(
        Long id,
        Long studentId,
        String studentName,
        String enrollmentNumber,
        Long credentialId,
        AccessEventType eventType,
        CaptureMethod captureMethod,
        OffsetDateTime occurredAt,
        String deviceName,
        String notes
) {
}