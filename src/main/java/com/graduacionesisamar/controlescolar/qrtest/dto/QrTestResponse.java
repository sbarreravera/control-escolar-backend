package com.graduacionesisamar.controlescolar.qrtest.dto;

import java.time.OffsetDateTime;

/**
 * Una consulta informativa: jamás representa un movimiento de acceso.
 * El token QR no se incluye en la respuesta.
 */
public record QrTestResponse(
        boolean valid,
        String status,
        Long studentId,
        String studentName,
        String enrollmentNumber,
        String schoolName,
        String gradeName,
        String groupName,
        OffsetDateTime credentialExpiresAt
) {}
