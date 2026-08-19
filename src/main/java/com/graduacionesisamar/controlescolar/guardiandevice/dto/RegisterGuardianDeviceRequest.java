package com.graduacionesisamar.controlescolar.guardiandevice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contains the information required to register a guardian device.
 */
public record RegisterGuardianDeviceRequest(

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