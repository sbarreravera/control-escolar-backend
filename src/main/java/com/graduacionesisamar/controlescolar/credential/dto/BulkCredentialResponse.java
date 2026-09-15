package com.graduacionesisamar.controlescolar.credential.dto;

/**
 * Represents one active student credential returned by the bulk QR workflow.
 */
public record BulkCredentialResponse(
        Long credentialId,
        Long studentId,
        String enrollmentNumber,
        String firstName,
        String lastName,
        String qrToken,
        boolean created
) {
}
