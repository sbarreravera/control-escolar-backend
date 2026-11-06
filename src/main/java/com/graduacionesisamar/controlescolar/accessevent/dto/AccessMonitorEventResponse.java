package com.graduacionesisamar.controlescolar.accessevent.dto;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;

import java.time.OffsetDateTime;

/**
 * A previously recorded school access; never includes a QR token or guardian data.
 */
public record AccessMonitorEventResponse(
        Long id,
        Long studentId,
        String studentName,
        String enrollmentNumber,
        String academicCycleName,
        String gradeName,
        String groupName,
        AccessEventType eventType,
        CaptureMethod captureMethod,
        OffsetDateTime occurredAt,
        String deviceName
) {}
