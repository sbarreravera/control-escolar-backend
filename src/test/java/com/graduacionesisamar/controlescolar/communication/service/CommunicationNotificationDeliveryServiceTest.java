package com.graduacionesisamar.controlescolar.communication.service;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationRecipientPushStatus;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunication;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunicationRecipient;
import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.notification.client.PushNotificationClient;
import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationStatus;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.notification.service.NotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationNotificationDeliveryServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private GuardianDeviceRepository guardianDeviceRepository;

    @Mock
    private SchoolCommunicationRecipientRepository recipientRepository;

    @Mock
    private PushNotificationClient pushNotificationClient;

    @InjectMocks
    private NotificationDeliveryService deliveryService;

    @Captor
    private ArgumentCaptor<PushNotificationRequest> requestCaptor;

    @Test
    void deliversCommunicationAndUpdatesRecipientState() {
        Guardian guardian = new Guardian();
        guardian.setId(10L);
        guardian.setFullName("Tutor Prueba");

        SchoolCommunication communication = new SchoolCommunication();
        communication.setId(20L);
        communication.setTitle("Cambio de horario");
        communication.setMessage("Mañana la salida será a las 12:00.");

        SchoolCommunicationRecipient recipient =
                new SchoolCommunicationRecipient();
        recipient.setId(30L);
        recipient.setGuardian(guardian);
        recipient.setCommunication(communication);
        recipient.setPushStatus(CommunicationRecipientPushStatus.PENDING);

        NotificationLog notification = new NotificationLog();
        notification.setId(40L);
        notification.setGuardian(guardian);
        notification.setCommunicationRecipient(recipient);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTitle(communication.getTitle());
        notification.setMessage(communication.getMessage());

        GuardianDevice device = new GuardianDevice();
        device.setId(50L);
        device.setGuardian(guardian);
        device.setFcmToken("valid-token");
        device.setActive(true);

        when(notificationLogRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                        NotificationStatus.PENDING
                ))
                .thenReturn(List.of(notification));
        when(guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(10L))
                .thenReturn(List.of(device));
        when(pushNotificationClient.send(any(PushNotificationRequest.class)))
                .thenReturn("firebase-id");

        int delivered = deliveryService.deliverPending();

        assertEquals(1, delivered);
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(
                CommunicationRecipientPushStatus.SENT,
                recipient.getPushStatus()
        );
        assertNotNull(recipient.getPushSentAt());

        verify(pushNotificationClient).send(requestCaptor.capture());
        PushNotificationRequest request = requestCaptor.getValue();
        assertEquals("20", request.data().get("communicationId"));
        assertEquals("COMMUNICATION", request.data().get("notificationType"));
        assertEquals(
                "/#/guardian/communications?communicationId=20",
                request.data().get("route")
        );
        verify(recipientRepository).save(recipient);
    }
}
