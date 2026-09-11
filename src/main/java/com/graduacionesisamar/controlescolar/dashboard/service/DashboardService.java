package com.graduacionesisamar.controlescolar.dashboard.service;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.dashboard.dto.DashboardRecentAccessEventResponse;
import com.graduacionesisamar.controlescolar.dashboard.dto.DashboardSummaryResponse;
import com.graduacionesisamar.controlescolar.dashboard.repository.DashboardRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Builds the operational dashboard for a school administrator.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_EVENT_LIMIT = 8;

    private final DashboardRepository dashboardRepository;
    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(
            Long schoolId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);
        validateRange(from, to);

        long activeStudents =
                dashboardRepository.countActiveStudents(schoolId);
        long activeGuardians =
                dashboardRepository.countActiveGuardians(schoolId);
        long studentsWithGuardian =
                dashboardRepository.countStudentsWithGuardian(schoolId);
        long activeCredentials =
                dashboardRepository.countActiveCredentials(schoolId);
        long activatedGuardianAccounts =
                dashboardRepository.countActivatedGuardianAccounts(schoolId);
        long guardiansWithNotifications =
                dashboardRepository.countGuardiansWithNotifications(schoolId);
        long entriesToday = dashboardRepository.countAccessEvents(
                schoolId,
                AccessEventType.ENTRY,
                from,
                to
        );
        long exitsToday = dashboardRepository.countAccessEvents(
                schoolId,
                AccessEventType.EXIT,
                from,
                to
        );

        List<DashboardRecentAccessEventResponse> recentEvents =
                dashboardRepository.findRecentAccessEvents(
                                schoolId,
                                from,
                                to,
                                RECENT_EVENT_LIMIT
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return new DashboardSummaryResponse(
                activeStudents,
                activeGuardians,
                studentsWithGuardian,
                activeCredentials,
                activatedGuardianAccounts,
                guardiansWithNotifications,
                entriesToday,
                exitsToday,
                recentEvents
        );
    }

    private DashboardRecentAccessEventResponse toResponse(
            AccessEvent event
    ) {
        Student student = event.getStudent();
        SchoolGroup schoolGroup = student.getSchoolGroup();

        String gradeName = schoolGroup == null
                ? student.getGradeName()
                : schoolGroup.getGradeName();
        String groupName = schoolGroup == null
                ? student.getGroupName()
                : schoolGroup.getGroupName();

        return new DashboardRecentAccessEventResponse(
                event.getId(),
                student.getId(),
                (student.getFirstName() + " " + student.getLastName()).trim(),
                student.getEnrollmentNumber(),
                gradeName,
                groupName,
                event.getEventType(),
                event.getOccurredAt()
        );
    }

    private void validateRange(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid dashboard date range"
            );
        }
    }
}
