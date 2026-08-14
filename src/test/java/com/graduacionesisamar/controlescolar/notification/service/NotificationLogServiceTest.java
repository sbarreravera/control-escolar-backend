package com.graduacionesisamar.controlescolar.notification.service;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @InjectMocks
    private NotificationLogService notificationLogService;

    @Captor
    private ArgumentCaptor<List<NotificationLog>> notificationLogsCaptor;

    @Test
    void queueForEntryCreatesOneNotificationPerGuardian() {
        Student student = createStudent(
                1L,
                "Samuel",
                "Barrera Vera"
        );

        AccessEvent event = createEvent(
                10L,
                student,
                AccessEventType.ENTRY
        );

        Guardian firstGuardian = createGuardian(
                1L,
                "Ana López"
        );

        Guardian secondGuardian = createGuardian(
                2L,
                "Carlos Pérez"
        );

        when(studentGuardianRepository
                .findAllByStudent_IdAndReceivesNotificationsTrueAndGuardian_ActiveTrueOrderByPrimaryContactDescGuardian_FullNameAsc(
                        student.getId()
                ))
                .thenReturn(List.of(
                        createRelationship(student, firstGuardian),
                        createRelationship(student, secondGuardian)
                ));

        int notificationsQueued =
                notificationLogService.queueForEvent(event);

        assertEquals(2, notificationsQueued);

        verify(notificationLogRepository)
                .saveAll(notificationLogsCaptor.capture());

        List<NotificationLog> notifications =
                notificationLogsCaptor.getValue();

        assertEquals(2, notifications.size());

        NotificationLog firstNotification =
                notifications.get(0);

        assertSame(
                event,
                firstNotification.getAccessEvent()
        );

        assertSame(
                firstGuardian,
                firstNotification.getGuardian()
        );

        assertEquals(
                "Entrada registrada",
                firstNotification.getTitle()
        );

        assertEquals(
                "Samuel Barrera Vera registró una entrada.",
                firstNotification.getMessage()
        );

        NotificationLog secondNotification =
                notifications.get(1);

        assertSame(
                event,
                secondNotification.getAccessEvent()
        );

        assertSame(
                secondGuardian,
                secondNotification.getGuardian()
        );
    }

    @Test
    void queueForExitCreatesCorrectNotification() {
        Student student = createStudent(
                1L,
                "Samuel",
                "Barrera Vera"
        );

        AccessEvent event = createEvent(
                11L,
                student,
                AccessEventType.EXIT
        );

        Guardian guardian = createGuardian(
                1L,
                "Ana López"
        );

        when(studentGuardianRepository
                .findAllByStudent_IdAndReceivesNotificationsTrueAndGuardian_ActiveTrueOrderByPrimaryContactDescGuardian_FullNameAsc(
                        student.getId()
                ))
                .thenReturn(List.of(
                        createRelationship(student, guardian)
                ));

        int notificationsQueued =
                notificationLogService.queueForEvent(event);

        assertEquals(1, notificationsQueued);

        verify(notificationLogRepository)
                .saveAll(notificationLogsCaptor.capture());

        NotificationLog notification =
                notificationLogsCaptor.getValue().get(0);

        assertEquals(
                "Salida registrada",
                notification.getTitle()
        );

        assertEquals(
                "Samuel Barrera Vera registró una salida.",
                notification.getMessage()
        );
    }

    @Test
    void queueForEventWithoutGuardiansReturnsZero() {
        Student student = createStudent(
                1L,
                "Samuel",
                "Barrera Vera"
        );

        AccessEvent event = createEvent(
                12L,
                student,
                AccessEventType.ENTRY
        );

        when(studentGuardianRepository
                .findAllByStudent_IdAndReceivesNotificationsTrueAndGuardian_ActiveTrueOrderByPrimaryContactDescGuardian_FullNameAsc(
                        student.getId()
                ))
                .thenReturn(List.of());

        int notificationsQueued =
                notificationLogService.queueForEvent(event);

        assertEquals(0, notificationsQueued);

        verify(notificationLogRepository)
                .saveAll(notificationLogsCaptor.capture());

        assertTrue(
                notificationLogsCaptor.getValue().isEmpty()
        );
    }

    private Student createStudent(
            Long id,
            String firstName,
            String lastName
    ) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        return student;
    }

    private Guardian createGuardian(
            Long id,
            String fullName
    ) {
        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setFullName(fullName);
        return guardian;
    }

    private AccessEvent createEvent(
            Long id,
            Student student,
            AccessEventType eventType
    ) {
        AccessEvent event = new AccessEvent();
        event.setId(id);
        event.setStudent(student);
        event.setEventType(eventType);
        return event;
    }

    private StudentGuardian createRelationship(
            Student student,
            Guardian guardian
    ) {
        StudentGuardian relationship =
                new StudentGuardian();

        relationship.setStudent(student);
        relationship.setGuardian(guardian);

        return relationship;
    }
}