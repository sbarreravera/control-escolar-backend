package com.graduacionesisamar.controlescolar.guardianportal.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;
import com.graduacionesisamar.controlescolar.accessevent.repository.AccessEventRepository;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianAccessEventPageResponse;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianAccessEventResponse;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianStudentResponse;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianPortalServiceTest {

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private AccessEventRepository accessEventRepository;

    @InjectMocks
    private GuardianPortalService guardianPortalService;

    @Test
    void findStudentsUsesGuardianAndSchoolFromTrustedPrincipal() {
        GuardianPrincipal principal = principal(20L, 10L);
        Student student = student(30L, 10L);
        StudentGuardian relationship = relationship(student);
        AccessEvent latestEvent = event(40L, student);

        when(studentGuardianRepository.findForGuardianPortal(20L, 10L))
                .thenReturn(List.of(relationship));
        when(accessEventRepository
                .findFirstByStudent_IdOrderByOccurredAtDescIdDesc(30L))
                .thenReturn(Optional.of(latestEvent));

        List<GuardianStudentResponse> result =
                guardianPortalService.findStudents(principal);

        assertEquals(1, result.size());
        GuardianStudentResponse response = result.getFirst();
        assertEquals(30L, response.studentId());
        assertEquals("Ana López", response.fullName());
        assertEquals("2026 - 2027", response.academicCycleName());
        assertEquals("5to Semestre", response.gradeName());
        assertEquals("Grupo 1", response.groupName());
        assertEquals(40L, response.latestEvent().id());

        verify(studentGuardianRepository)
                .findForGuardianPortal(20L, 10L);
    }

    @Test
    void findHistoryCannotReplaceGuardianOrSchoolWithRequestData() {
        GuardianPrincipal principal = principal(20L, 10L);
        Student student = student(30L, 10L);
        AccessEvent accessEvent = event(40L, student);
        OffsetDateTime occurredFrom = OffsetDateTime.parse(
                "2026-09-01T00:00:00-06:00"
        );
        OffsetDateTime occurredTo = OffsetDateTime.parse(
                "2026-10-01T00:00:00-06:00"
        );

        when(accessEventRepository.findGuardianHistory(
                eq(20L),
                eq(10L),
                eq(30L),
                eq(AccessEventType.ENTRY),
                eq(occurredFrom),
                eq(occurredTo),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(accessEvent)));

        GuardianAccessEventPageResponse result =
                guardianPortalService.findHistory(
                        principal,
                        0,
                        20,
                        30L,
                        AccessEventType.ENTRY,
                        occurredFrom,
                        occurredTo
                );

        assertEquals(1, result.totalElements());
        assertEquals(40L, result.content().getFirst().id());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(accessEventRepository).findGuardianHistory(
                eq(20L),
                eq(10L),
                eq(30L),
                eq(AccessEventType.ENTRY),
                eq(occurredFrom),
                eq(occurredTo),
                pageableCaptor.capture()
        );

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void findEventReturnsNotFoundWhenEventIsOutsideSessionScope() {
        GuardianPrincipal principal = principal(20L, 10L);

        when(accessEventRepository.findGuardianEvent(999L, 20L, 10L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> guardianPortalService.findEvent(principal, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(accessEventRepository)
                .findGuardianEvent(999L, 20L, 10L);
    }

    @Test
    void findHistoryRejectsAnInvertedDateRange() {
        GuardianPrincipal principal = principal(20L, 10L);
        OffsetDateTime occurredFrom = OffsetDateTime.parse(
                "2026-10-01T00:00:00-06:00"
        );
        OffsetDateTime occurredTo = OffsetDateTime.parse(
                "2026-09-01T00:00:00-06:00"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> guardianPortalService.findHistory(
                        principal,
                        0,
                        20,
                        null,
                        null,
                        occurredFrom,
                        occurredTo
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void findStudentsKeepsHistoricalStudentWithoutCurrentGroup() {
        GuardianPrincipal principal = principal(20L, 10L);
        Student student = student(30L, 10L);
        student.setActive(false);
        student.setSchoolGroup(null);
        student.setGradeName("Egresado");
        student.setGroupName(null);

        when(studentGuardianRepository.findForGuardianPortal(20L, 10L))
                .thenReturn(List.of(relationship(student)));
        when(accessEventRepository
                .findFirstByStudent_IdOrderByOccurredAtDescIdDesc(30L))
                .thenReturn(Optional.empty());

        GuardianStudentResponse response = guardianPortalService
                .findStudents(principal)
                .getFirst();

        assertFalse(response.active());
        assertEquals("Egresado", response.gradeName());
        assertNull(response.academicCycleId());
        assertNull(response.latestEvent());
    }

    private GuardianPrincipal principal(
            Long guardianId,
            Long schoolId
    ) {
        return new GuardianPrincipal(
                1L,
                guardianId,
                schoolId,
                "María Pérez",
                "Colegio San Felipe de Jesús",
                OffsetDateTime.parse("2026-10-09T10:00:00-06:00")
        );
    }

    private Student student(Long studentId, Long schoolId) {
        School school = new School();
        school.setId(schoolId);
        school.setName("Colegio San Felipe de Jesús");

        AcademicCycle academicCycle = new AcademicCycle();
        academicCycle.setId(60L);
        academicCycle.setSchool(school);
        academicCycle.setName("2026 - 2027");
        academicCycle.setStartDate(LocalDate.of(2026, 8, 1));
        academicCycle.setEndDate(LocalDate.of(2027, 7, 31));

        SchoolGroup schoolGroup = new SchoolGroup();
        schoolGroup.setId(50L);
        schoolGroup.setAcademicCycle(academicCycle);
        schoolGroup.setGradeName("5to Semestre");
        schoolGroup.setGroupName("Grupo 1");

        Student student = new Student();
        student.setId(studentId);
        student.setSchool(school);
        student.setEnrollmentNumber("241130709010260");
        student.setFirstName("Ana");
        student.setLastName("López");
        student.setSchoolGroup(schoolGroup);
        student.setActive(true);
        return student;
    }

    private StudentGuardian relationship(Student student) {
        Guardian guardian = new Guardian();
        guardian.setId(20L);
        guardian.setSchool(student.getSchool());
        guardian.setFullName("María Pérez");

        StudentGuardian relationship = new StudentGuardian();
        relationship.setStudent(student);
        relationship.setGuardian(guardian);
        relationship.setRelationship("Madre");
        relationship.setPrimaryContact(true);
        return relationship;
    }

    private AccessEvent event(Long eventId, Student student) {
        AccessEvent event = new AccessEvent();
        event.setId(eventId);
        event.setStudent(student);
        event.setEventType(AccessEventType.ENTRY);
        event.setCaptureMethod(CaptureMethod.QR_CAMERA);
        event.setOccurredAt(OffsetDateTime.parse(
                "2026-09-09T08:15:00-06:00"
        ));
        return event;
    }
}
