package com.graduacionesisamar.controlescolar.auth.dto;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;

import java.util.List;

public record AuthenticatedUserResponse(
        Long id,
        Long schoolId,
        String schoolName,
        String fullName,
        String email,
        AppUserRole role,
        List<String> moduleKeys
) {
}