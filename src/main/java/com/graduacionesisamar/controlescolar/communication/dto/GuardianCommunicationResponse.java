package com.graduacionesisamar.controlescolar.communication.dto;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationCategory;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationPriority;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationType;

import java.time.OffsetDateTime;

public record GuardianCommunicationResponse(
        Long id,
        CommunicationType type,
        CommunicationCategory category,
        CommunicationPriority priority,
        String title,
        String message,
        boolean requiresAcknowledgement,
        OffsetDateTime publishedAt,
        OffsetDateTime viewedAt,
        OffsetDateTime acknowledgedAt
) {
}
