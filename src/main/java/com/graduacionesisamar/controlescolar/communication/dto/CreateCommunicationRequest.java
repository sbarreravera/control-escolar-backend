package com.graduacionesisamar.controlescolar.communication.dto;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationCategory;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationPriority;
import com.graduacionesisamar.controlescolar.communication.entity.CommunicationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateCommunicationRequest(
        @NotNull CommunicationType type,
        @NotNull CommunicationCategory category,
        @NotNull CommunicationPriority priority,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String message,
        boolean requiresAcknowledgement,
        @Valid @NotNull CommunicationAudienceRequest audience,
        OffsetDateTime scheduledAt
) {
}
