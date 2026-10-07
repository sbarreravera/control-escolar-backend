package com.graduacionesisamar.controlescolar.schooluser.dto;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;

import java.time.OffsetDateTime;
import java.util.List;

public record SchoolUserResponse(
        Long id,
        Long schoolId,
        String fullName,
        String email,
        AppUserRole role,
        boolean active,
        List<String> moduleKeys,
        boolean editable,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
