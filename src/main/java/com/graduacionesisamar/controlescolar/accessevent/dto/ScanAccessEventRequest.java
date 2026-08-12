package com.graduacionesisamar.controlescolar.accessevent.dto;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contains the information received from a QR scan.
 */
public record ScanAccessEventRequest(

        @NotBlank(message = "QR token is required")
        @Size(max = 100, message = "QR token cannot exceed 100 characters")
        String qrToken,

        @NotNull(message = "Event type is required")
        AccessEventType eventType,

        @NotNull(message = "Capture method is required")
        CaptureMethod captureMethod,

        @Size(max = 100, message = "Device name cannot exceed 100 characters")
        String deviceName,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {
}