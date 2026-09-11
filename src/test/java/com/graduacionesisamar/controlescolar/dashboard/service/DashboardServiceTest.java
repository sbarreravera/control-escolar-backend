package com.graduacionesisamar.controlescolar.dashboard.service;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.dashboard.dto.DashboardSummaryResponse;
import com.graduacionesisamar.controlescolar.dashboard.repository.DashboardRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                dashboardRepository,
                schoolAccessService
        );
    }

    @Test
    void buildsOperationalSummaryForSchool() {
        Long schoolId = 2L;
        OffsetDateTime from = OffsetDateTime.parse(
                "2026-09-11T00:00:00-06:00"
        );
        OffsetDateTime to = OffsetDateTime.parse(
                "2026-09-12T00:00:00-06:00"
        );

        when(dashboardRepository.countActiveStudents(schoolId))
                .thenReturn(221L);
        when(dashboardRepository.countActiveGuardians(schoolId))
                .thenReturn(180L);
        when(dashboardRepository.countStudentsWithGuardian(schoolId))
                .thenReturn(205L);
        when(dashboardRepository.countActiveCredentials(schoolId))
                .thenReturn(198L);
        when(dashboardRepository.countActivatedGuardianAccounts(schoolId))
                .thenReturn(160L);
        when(dashboardRepository.countGuardiansWithNotifications(schoolId))
                .thenReturn(145L);
        when(dashboardRepository.countAccessEvents(
                schoolId,
                AccessEventType.ENTRY,
                from,
                to
        )).thenReturn(120L);
        when(dashboardRepository.countAccessEvents(
                schoolId,
                AccessEventType.EXIT,
                from,
                to
        )).thenReturn(80L);

        Student student = new Student();
        student.setId(50L);
        student.setFirstName("Ana");
        student.setLastName("Pérez López");
        student.setEnrollmentNumber("MAT-050");

        SchoolGroup group = new SchoolGroup();
        group.setGradeName("1er Semestre");
        group.setGroupName("GPO 1");
        student.setSchoolGroup(group);

        AccessEvent event = new AccessEvent();
        event.setId(70L);
        event.setStudent(student);
        event.setEventType(AccessEventType.ENTRY);
        event.setOccurredAt(from.plusHours(8));

        when(dashboardRepository.findRecentAccessEvents(
                schoolId,
                from,
                to,
                8
        )).thenReturn(List.of(event));

        DashboardSummaryResponse response =
                dashboardService.getSummary(schoolId, from, to);

        verify(schoolAccessService).requireAccessToSchool(schoolId);
        assertEquals(221L, response.activeStudents());
        assertEquals(180L, response.activeGuardians());
        assertEquals(205L, response.studentsWithGuardian());
        assertEquals(198L, response.activeCredentials());
        assertEquals(160L, response.activatedGuardianAccounts());
        assertEquals(145L, response.guardiansWithNotifications());
        assertEquals(120L, response.entriesToday());
        assertEquals(80L, response.exitsToday());
        assertEquals(1, response.recentEvents().size());
        assertEquals("Ana Pérez López", response.recentEvents().getFirst().studentName());
        assertEquals("1er Semestre", response.recentEvents().getFirst().gradeName());
        assertEquals("GPO 1", response.recentEvents().getFirst().groupName());
    }

    @Test
    void rejectsInvalidDateRange() {
        Long schoolId = 2L;
        OffsetDateTime from = OffsetDateTime.parse(
                "2026-09-11T00:00:00-06:00"
        );

        assertThrows(
                ResponseStatusException.class,
                () -> dashboardService.getSummary(
                        schoolId,
                        from,
                        from
                )
        );

        verify(schoolAccessService).requireAccessToSchool(schoolId);
    }
}
