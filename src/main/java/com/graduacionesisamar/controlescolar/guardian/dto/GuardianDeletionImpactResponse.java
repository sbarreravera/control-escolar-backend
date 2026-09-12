package com.graduacionesisamar.controlescolar.guardian.dto;

import java.util.List;

public record GuardianDeletionImpactResponse(
        Long guardianId,
        String guardianName,
        String externalReference,
        List<GuardianDeletionStudentResponse> students,
        long activeNotificationDeviceCount,
        long notificationLogCount,
        long communicationRecipientCount
) {
}
