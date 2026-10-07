package com.graduacionesisamar.controlescolar.schooluser.dto;

public record SchoolModuleResponse(
        String key,
        String name,
        String description,
        boolean defaultGranted,
        int order
) {
}
