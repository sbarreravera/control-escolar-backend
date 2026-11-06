package com.graduacionesisamar.controlescolar.accessevent.dto;

import java.util.List;

/**
 * Filter metadata available under ACCESS_MONITOR without requiring the
 * independent academic-structure administration permission.
 */
public record AccessMonitorFilterOptions(
        List<CycleOption> cycles,
        List<GroupOption> groups
) {
    public record CycleOption(Long id, String name, boolean active) {}
    public record GroupOption(
            Long id,
            Long academicCycleId,
            String gradeName,
            String groupName,
            boolean active
    ) {}
}
