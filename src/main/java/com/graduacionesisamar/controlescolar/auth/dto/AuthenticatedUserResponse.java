package com.graduacionesisamar.controlescolar.auth.dto;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;

public record AuthenticatedUserResponse(
        Long id,
        Long schoolId,
        String schoolName,
        String fullName,
        String email,
        AppUserRole role
) {
}