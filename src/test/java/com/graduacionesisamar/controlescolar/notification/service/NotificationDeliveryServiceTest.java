package com.graduacionesisamar.controlescolar.notification.service;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.notification.client.PushNotificationClient;
import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationStatus;
import com.graduacionesisamar.controlescolar.notification.exception.PushNotificationException;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private GuardianDeviceRepository guardianDeviceRepository;

    @Mock
    private PushNotificationClient pushNotificationClient;

    @InjectMocks
    private NotificationDeliveryService notificationDeliveryService;

    @Captor
    private ArgumentCaptor<PushNotificationRequest> requestCaptor;

    @Test
    void deliverPendingSendsNotificationToActiveDevice() {
        NotificationLog notification = createNotification();
        GuardianDevice device = createDevice(
                1L,
                notification.getGuardian(),
                "test-token-1"
        );

        when(notificationLogRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                        NotificationStatus.PENDING
                ))
                .thenReturn(List.of(notification));

        when(guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        notification.getGuardian().getId()
                ))
                .thenReturn(List.of(device));

        when(pushNotificationClient.send(
                any(PushNotificationRequest.class)
        )).thenReturn("firebase-message-1");

        int delivered =
                notificationDeliveryService.deliverPending();

        assertEquals(1, delivered);
        assertEquals(
                NotificationStatus.SENT,
                notification.getStatus()
        );
        assertNotNull(notification.getSentAt());
        assertNull(notification.getErrorMessage());
        assertNotNull(device.getLastUsedAt());
        assertTrue(device.isActive());

        verify(pushNotificationClient)
                .send(requestCaptor.capture());

        PushNotificationRequest request =
                requestCaptor.getValue();

        assertEquals("test-token-1", request.token());
        assertEquals(
                "Entrada registrada",
                request.title()
        );
        assertEquals(
                "Samuel Barrera Vera registró una entrada.",
                request.body()
        );
        assertEquals(
                "100",
                request.data().get("notificationLogId")
        );
        assertEquals(
                "200",
                request.data().get("accessEventId")
        );
        assertEquals(
                "300",
                request.data().get("studentId")
        );
        assertEquals(
                "ENTRY",
                request.data().get("eventType")
        );
        assertEquals(
            notification.getAccessEvent()
                .getOccurredAt()
                .toString(),
            request.data().get("occurredAt")
        );

        verify(guardianDeviceRepository)
                .saveAll(List.of(device));

        verify(notificationLogRepository)
                .save(notification);
    }

    @Test
    void deliverPendingFailsWhenGuardianHasNoActiveDevices() {
        NotificationLog notification = createNotification();

        when(notificationLogRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                        NotificationStatus.PENDING
                ))
                .thenReturn(List.of(notification));

        when(guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        notification.getGuardian().getId()
                ))
                .thenReturn(List.of());

        int delivered =
                notificationDeliveryService.deliverPending();

        assertEquals(0, delivered);
        assertEquals(
                NotificationStatus.FAILED,
                notification.getStatus()
        );
        assertNull(notification.getSentAt());
        assertEquals(
                "Guardian has no active registered devices",
                notification.getErrorMessage()
        );

        verify(notificationLogRepository)
                .save(notification);

        verifyNoInteractions(pushNotificationClient);
    }

    @Test
    void deliverPendingDeactivatesInvalidToken() {
        NotificationLog notification = createNotification();
        GuardianDevice device = createDevice(
                1L,
                notification.getGuardian(),
                "invalid-token"
        );

        when(notificationLogRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                        NotificationStatus.PENDING
                ))
                .thenReturn(List.of(notification));

        when(guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        notification.getGuardian().getId()
                ))
                .thenReturn(List.of(device));

        when(pushNotificationClient.send(
                any(PushNotificationRequest.class)
        )).thenThrow(new PushNotificationException(
                "Unable to send Firebase notification",
                true,
                new RuntimeException("Unregistered token")
        ));

        int delivered =
                notificationDeliveryService.deliverPending();

        assertEquals(0, delivered);
        assertFalse(device.isActive());
        assertNull(device.getLastUsedAt());
        assertEquals(
                NotificationStatus.FAILED,
                notification.getStatus()
        );
        assertNull(notification.getSentAt());
        assertEquals(
                "Unable to send Firebase notification",
                notification.getErrorMessage()
        );

        verify(guardianDeviceRepository)
                .saveAll(List.of(device));

        verify(notificationLogRepository)
                .save(notification);
    }

    @Test
    void deliverPendingMarksNotificationSentWhenOneDeviceSucceeds() {
        NotificationLog notification = createNotification();

        GuardianDevice validDevice = createDevice(
                1L,
                notification.getGuardian(),
                "valid-token"
        );

        GuardianDevice invalidDevice = createDevice(
                2L,
                notification.getGuardian(),
                "invalid-token"
        );

        when(notificationLogRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                        NotificationStatus.PENDING
                ))
                .thenReturn(List.of(notification));

        when(guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        notification.getGuardian().getId()
                ))
                .thenReturn(List.of(
                        validDevice,
                        invalidDevice
                ));

        when(pushNotificationClient.send(
                any(PushNotificationRequest.class)
        ))
                .thenReturn("firebase-message-1")
                .thenThrow(new PushNotificationException(
                        "Unable to send Firebase notification",
                        true,
                        new RuntimeException("Unregistered token")
                ));

        int delivered =
                notificationDeliveryService.deliverPending();

        assertEquals(1, delivered);
        assertEquals(
                NotificationStatus.SENT,
                notification.getStatus()
        );
        assertNotNull(notification.getSentAt());
        assertEquals(
                "1 device delivery attempt(s) failed",
                notification.getErrorMessage()
        );

        assertTrue(validDevice.isActive());
        assertNotNull(validDevice.getLastUsedAt());

        assertFalse(invalidDevice.isActive());
        assertNull(invalidDevice.getLastUsedAt());

        verify(guardianDeviceRepository)
                .saveAll(List.of(
                        validDevice,
                        invalidDevice
                ));

        verify(notificationLogRepository)
                .save(notification);
    }

    private NotificationLog createNotification() {
        Guardian guardian = new Guardian();
        guardian.setId(400L);
        guardian.setFullName("Tutor de prueba");

        Student student = new Student();
        student.setId(300L);
        student.setFirstName("Samuel");
        student.setLastName("Barrera Vera");

        AccessEvent event = new AccessEvent();
        event.setId(200L);
        event.setStudent(student);
        event.setEventType(AccessEventType.ENTRY);
        event.setOccurredAt(
                OffsetDateTime.parse(
                        "2026-08-19T08:30:00-06:00"
                )
        );

        NotificationLog notification = new NotificationLog();
        notification.setId(100L);
        notification.setAccessEvent(event);
        notification.setGuardian(guardian);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTitle("Entrada registrada");
        notification.setMessage(
                "Samuel Barrera Vera registró una entrada."
        );

        return notification;
    }

    private GuardianDevice createDevice(
            Long id,
            Guardian guardian,
            String fcmToken
    ) {
        GuardianDevice device = new GuardianDevice();
        device.setId(id);
        device.setGuardian(guardian);
        device.setFcmToken(fcmToken);
        device.setDeviceName("Dispositivo de prueba");
        device.setActive(true);
        return device;
    }
}