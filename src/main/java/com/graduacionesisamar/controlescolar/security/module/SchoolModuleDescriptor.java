package com.graduacionesisamar.controlescolar.security.module;

public record SchoolModuleDescriptor(
        String key,
        String name,
        String description,
        boolean defaultGranted,
        int order
) {
}
