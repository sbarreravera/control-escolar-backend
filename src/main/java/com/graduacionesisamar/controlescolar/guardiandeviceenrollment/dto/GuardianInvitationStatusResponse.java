package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto;

import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianEnrollmentPurpose;

import java.time.OffsetDateTime;

/**
 * Safe public metadata used to render a one-time invitation.
 */
public record GuardianInvitationStatusResponse(
        String status,
        GuardianEnrollmentPurpose purpose,
        Long guardianId,
        String guardianName,
        String schoolName,
        String schoolCode,
        String username,
        boolean accountActivated,
        OffsetDateTime expiresAt
) {
}
