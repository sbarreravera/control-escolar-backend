package com.graduacionesisamar.controlescolar.guardianaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credentials used to recreate a guardian session on any device.
 */
public record GuardianLoginRequest(
        @NotBlank(message = "School code is required")
        @Size(max = 50, message = "School code must not exceed 50 characters")
        String schoolCode,

        @NotBlank(message = "Username is required")
        @Size(max = 100, message = "Username must not exceed 100 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 72,
                message = "Password must contain between 8 and 72 characters"
        )
        String password,

        @Size(max = 512, message = "FCM token must not exceed 512 characters")
        String fcmToken,

        @Size(max = 100, message = "Device name must not exceed 100 characters")
        String deviceName
) {
}
