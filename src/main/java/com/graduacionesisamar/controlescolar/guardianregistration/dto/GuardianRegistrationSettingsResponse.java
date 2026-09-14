package com.graduacionesisamar.controlescolar.guardianregistration.dto;

public record GuardianRegistrationSettingsResponse(
        Long schoolId,
        String schoolName,
        Boolean enabled,
        Integer minimumGuardiansPerStudent,
        Integer maximumGuardiansPerStudent,
        String registrationToken
) {
}
