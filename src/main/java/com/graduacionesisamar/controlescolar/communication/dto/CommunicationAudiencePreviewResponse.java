package com.graduacionesisamar.controlescolar.communication.dto;

public record CommunicationAudiencePreviewResponse(
        int students,
        int guardians,
        int guardiansWithPush,
        int guardiansWithoutPush,
        String audienceSummary
) {
}
