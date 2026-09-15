package com.graduacionesisamar.controlescolar.guardianregistration.dto;

public record GuardianRegistrationContextResponse(
        String schoolName,
        String schoolCode,
        Integer minimumGuardiansPerStudent,
        Integer maximumGuardiansPerStudent
) {
}
