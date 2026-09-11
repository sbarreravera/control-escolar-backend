package com.graduacionesisamar.controlescolar.dashboard.dto;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;

import java.time.OffsetDateTime;

/**
 * One recent school access movement shown in the operational dashboard.
 */
public record DashboardRecentAccessEventResponse(
        Long id,
        Long studentId,
        String studentName,
        String enrollmentNumber,
        String gradeName,
        String groupName,
        AccessEventType eventType,
        OffsetDateTime occurredAt
) {
}
