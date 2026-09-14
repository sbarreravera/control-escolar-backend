package com.graduacionesisamar.controlescolar.guardianregistration.dto;

import java.util.List;

public record GuardianSelfRegistrationResponse(
        Long guardianId,
        String guardianReference,
        String username,
        String schoolName,
        String schoolCode,
        List<String> studentEnrollmentNumbers
) {
}
