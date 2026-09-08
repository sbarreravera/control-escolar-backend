package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contains the information sent by the guardian's browser
 * when completing device enrollment.
 */
public record CompleteGuardianDeviceEnrollmentRequest(

        @NotBlank(message = "Enrollment token is required")
        @Size(
                max = 100,
                message = "Enrollment token must not exceed 100 characters"
        )
        String enrollmentToken,

        @NotBlank(message = "FCM token is required")
        @Size(
                max = 512,
                message = "FCM token must not exceed 512 characters"
        )
        String fcmToken,

        @Size(
                max = 100,
                message = "Device name must not exceed 100 characters"
        )
        String deviceName

) {
}