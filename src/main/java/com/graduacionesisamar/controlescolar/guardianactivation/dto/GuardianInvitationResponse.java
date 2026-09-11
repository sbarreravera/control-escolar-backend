package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.time.OffsetDateTime;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianEnrollmentPurpose;

/**
 * One generated invitation. The raw token is returned once and never stored.
 */
public record GuardianInvitationResponse(
        Long guardianId,
        String externalReference,
        String guardianName,
        String phone,
        String email,
        String schoolName,
        String schoolCode,
        String username,
        GuardianEnrollmentPurpose purpose,
        String enrollmentToken,
        OffsetDateTime expiresAt
) {
}
