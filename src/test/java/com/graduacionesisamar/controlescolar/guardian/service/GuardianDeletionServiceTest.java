package com.graduacionesisamar.controlescolar.guardian.service;

import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianDeletionImpactResponse;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianDeletionServiceTest {

    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    @Mock
    private GuardianDeviceRepository guardianDeviceRepository;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private SchoolCommunicationRecipientRepository communicationRecipientRepository;
    @Mock
    private SchoolAccessService schoolAccessService;

    private GuardianDeletionService service;
    private Guardian guardian;

    @BeforeEach
    void setUp() {
        service = new GuardianDeletionService(
                guardianRepository,
                studentGuardianRepository,
                guardianDeviceRepository,
                notificationLogRepository,
                communicationRecipientRepository,
                schoolAccessService
        );

        School school = new School();
        school.setId(10L);
        school.setName("Escuela de Prueba");

        guardian = new Guardian();
        guardian.setId(20L);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de Prueba");
        guardian.setExternalReference("EJE-02");
    }

    @Test
    void previewReportsStudentsNotificationsAndHistory() {
        Student student = new Student();
        student.setId(30L);
        student.setFirstName("Alumno");
        student.setLastName("Ejemplo");
        student.setEnrollmentNumber("MAT-001");

        StudentGuardian relationship = new StudentGuardian();
        relationship.setStudent(student);

        when(guardianRepository.findById(20L))
                .thenReturn(Optional.of(guardian));
        when(studentGuardianRepository
                .findAllByGuardian_IdOrderByStudent_LastNameAscStudent_FirstNameAsc(20L))
                .thenReturn(List.of(relationship));
        when(guardianDeviceRepository.countByGuardian_IdAndActiveTrue(20L))
                .thenReturn(1L);
        when(notificationLogRepository.countByGuardian_Id(20L))
                .thenReturn(3L);
        when(communicationRecipientRepository.countByGuardian_Id(20L))
                .thenReturn(2L);

        GuardianDeletionImpactResponse impact = service.preview(20L);

        assertEquals(20L, impact.guardianId());
        assertEquals("Tutor de Prueba", impact.guardianName());
        assertEquals("EJE-02", impact.externalReference());
        assertEquals(1, impact.students().size());
        assertEquals("Alumno Ejemplo", impact.students().getFirst().studentName());
        assertEquals("MAT-001", impact.students().getFirst().enrollmentNumber());
        assertEquals(1L, impact.activeNotificationDeviceCount());
        assertEquals(3L, impact.notificationLogCount());
        assertEquals(2L, impact.communicationRecipientCount());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void deleteRemovesNonCascadingHistoryBeforeGuardian() {
        when(guardianRepository.findById(20L))
                .thenReturn(Optional.of(guardian));

        service.delete(20L);

        verify(schoolAccessService).requireAccessToSchool(10L);
        verify(notificationLogRepository).deleteAllForGuardian(20L);
        verify(communicationRecipientRepository).deleteAllForGuardian(20L);
        verify(guardianRepository).delete(guardian);
        verify(guardianRepository).flush();
    }

    @Test
    void deleteRejectsUnknownGuardianWithoutTouchingHistory() {
        when(guardianRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(99L)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(notificationLogRepository, never()).deleteAllForGuardian(99L);
        verify(communicationRecipientRepository, never()).deleteAllForGuardian(99L);
    }
}
