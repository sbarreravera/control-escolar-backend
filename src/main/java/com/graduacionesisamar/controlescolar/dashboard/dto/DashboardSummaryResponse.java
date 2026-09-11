package com.graduacionesisamar.controlescolar.dashboard.dto;

import java.util.List;

/**
 * Operational indicators for one school dashboard.
 */
public record DashboardSummaryResponse(
        long activeStudents,
        long activeGuardians,
        long studentsWithGuardian,
        long activeCredentials,
        long activatedGuardianAccounts,
        long guardiansWithNotifications,
        long entriesToday,
        long exitsToday,
        List<DashboardRecentAccessEventResponse> recentEvents
) {
}
