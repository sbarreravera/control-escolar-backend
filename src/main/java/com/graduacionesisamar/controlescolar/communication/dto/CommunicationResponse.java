package com.graduacionesisamar.controlescolar.communication.dto;

import com.graduacionesisamar.controlescolar.communication.entity.*;

import java.time.OffsetDateTime;

public record CommunicationResponse(
        Long id,
        Long schoolId,
        String createdByName,
        CommunicationType type,
        CommunicationCategory category,
        CommunicationPriority priority,
        CommunicationStatus status,
        CommunicationAudienceType audienceType,
        String audienceSummary,
        String title,
        String message,
        boolean requiresAcknowledgement,
        int studentCount,
        int recipientCount,
        int pushRecipientCount,
        long sentCount,
        long failedCount,
        long viewedCount,
        long acknowledgedCount,
        OffsetDateTime scheduledAt,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
) {
}
