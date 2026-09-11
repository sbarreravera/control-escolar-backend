package com.graduacionesisamar.controlescolar.communication.dto;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationRecipientPushStatus;

import java.time.OffsetDateTime;

public record CommunicationRecipientResponse(
        Long recipientId,
        Long guardianId,
        String guardianName,
        String phone,
        String email,
        CommunicationRecipientPushStatus pushStatus,
        OffsetDateTime pushSentAt,
        OffsetDateTime viewedAt,
        OffsetDateTime acknowledgedAt
) {
}
