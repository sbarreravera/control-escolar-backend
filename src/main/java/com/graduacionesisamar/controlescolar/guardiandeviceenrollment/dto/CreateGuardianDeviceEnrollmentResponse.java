
package com.graduacionesisamar.controlescolar
        .guardiandeviceenrollment.dto;

import java.time.OffsetDateTime;

/**
 * Contains the temporary token returned when an administrator
 * creates a guardian device enrollment invitation.
 */
public record CreateGuardianDeviceEnrollmentResponse(

        Long guardianId,

        String guardianName,

        String schoolName,

        String enrollmentToken,

        OffsetDateTime expiresAt

) {
}