package com.graduacionesisamar.controlescolar.notification.service;

import com.graduacionesisamar.controlescolar.communication.entity.CommunicationRecipientPushStatus;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunication;
import com.graduacionesisamar.controlescolar.communication.entity.SchoolCommunicationRecipient;
import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.notification.client.PushNotificationClient;
import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationStatus;
import com.graduacionesisamar.controlescolar.notification.exception.PushNotificationException;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Delivers pending notifications to active guardian devices.
 */
@Service
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
public class NotificationDeliveryService {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final NotificationLogRepository notificationLogRepository;
    private final GuardianDeviceRepository guardianDeviceRepository;
    private final SchoolCommunicationRecipientRepository recipientRepository;
    private final PushNotificationClient pushNotificationClient;

    public int deliverPending() {
        List<NotificationLog> pendingNotifications = notificationLogRepository
                .findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING);

        int deliveredNotifications = 0;
        for (NotificationLog notification : pendingNotifications) {
            if (deliver(notification)) {
                deliveredNotifications++;
            }
        }
        return deliveredNotifications;
    }

    private boolean deliver(NotificationLog notification) {
        List<GuardianDevice> devices = guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        notification.getGuardian().getId()
                );

        if (devices.isEmpty()) {
            String message = "Guardian has no active registered devices";
            markAsFailed(notification, message);
            updateCommunicationRecipientFailed(notification, message);
            return false;
        }

        int successfulDeliveries = 0;
        List<String> deliveryErrors = new ArrayList<>();
        OffsetDateTime deliveryTime = OffsetDateTime.now();

        for (GuardianDevice device : devices) {
            try {
                pushNotificationClient.send(buildRequest(notification, device));
                device.setLastUsedAt(deliveryTime);
                successfulDeliveries++;
            } catch (PushNotificationException exception) {
                if (exception.isInvalidToken()) {
                    device.setActive(false);
                }
                deliveryErrors.add(exception.getMessage());
            }
        }

        guardianDeviceRepository.saveAll(devices);

        if (successfulDeliveries > 0) {
            markAsSent(notification, deliveryTime, deliveryErrors.size());
            updateCommunicationRecipientSent(
                    notification,
                    deliveryTime,
                    deliveryErrors.size()
            );
            return true;
        }

        String failure = buildFailureMessage(deliveryErrors);
        markAsFailed(notification, failure);
        updateCommunicationRecipientFailed(notification, failure);
        return false;
    }

    private PushNotificationRequest buildRequest(
            NotificationLog notification,
            GuardianDevice device
    ) {
        if (notification.getAccessEvent() != null) {
            return new PushNotificationRequest(
                    device.getFcmToken(),
                    notification.getTitle(),
                    notification.getMessage(),
                    Map.of(
                            "notificationLogId", String.valueOf(notification.getId()),
                            "accessEventId", String.valueOf(notification.getAccessEvent().getId()),
                            "studentId", String.valueOf(notification.getAccessEvent().getStudent().getId()),
                            "eventType", notification.getAccessEvent().getEventType().name(),
                            "occurredAt", notification.getAccessEvent().getOccurredAt().toString(),
                            "route", "/#/guardian?eventId=" + notification.getAccessEvent().getId()
                    )
            );
        }

        SchoolCommunicationRecipient recipient =
                notification.getCommunicationRecipient();
        SchoolCommunication communication = recipient.getCommunication();
        return new PushNotificationRequest(
                device.getFcmToken(),
                notification.getTitle(),
                notification.getMessage(),
                Map.of(
                        "notificationLogId", String.valueOf(notification.getId()),
                        "communicationId", String.valueOf(communication.getId()),
                        "communicationRecipientId", String.valueOf(recipient.getId()),
                        "notificationType", "COMMUNICATION",
                        "route", "/#/guardian?communicationId=" + communication.getId()
                )
        );
    }

    private void updateCommunicationRecipientSent(
            NotificationLog notification,
            OffsetDateTime sentAt,
            int failedDevices
    ) {
        SchoolCommunicationRecipient recipient =
                notification.getCommunicationRecipient();
        if (recipient == null) {
            return;
        }
        recipient.setPushStatus(CommunicationRecipientPushStatus.SENT);
        recipient.setPushSentAt(sentAt);
        recipient.setPushError(
                failedDevices == 0
                        ? null
                        : "%d intento(s) de entrega a dispositivo fallaron"
                        .formatted(failedDevices)
        );
        recipientRepository.save(recipient);
    }

    private void updateCommunicationRecipientFailed(
            NotificationLog notification,
            String error
    ) {
        SchoolCommunicationRecipient recipient =
                notification.getCommunicationRecipient();
        if (recipient == null) {
            return;
        }
        recipient.setPushStatus(CommunicationRecipientPushStatus.FAILED);
        recipient.setPushSentAt(null);
        recipient.setPushError(truncateError(error));
        recipientRepository.save(recipient);
    }

    private void markAsSent(
            NotificationLog notification,
            OffsetDateTime sentAt,
            int failedDevices
    ) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(sentAt);
        notification.setErrorMessage(
                failedDevices == 0
                        ? null
                        : "%d device delivery attempt(s) failed".formatted(failedDevices)
        );
        notificationLogRepository.save(notification);
    }

    private void markAsFailed(
            NotificationLog notification,
            String errorMessage
    ) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setSentAt(null);
        notification.setErrorMessage(truncateError(errorMessage));
        notificationLogRepository.save(notification);
    }

    private String buildFailureMessage(List<String> deliveryErrors) {
        if (deliveryErrors.isEmpty()) {
            return "Notification could not be delivered";
        }
        return truncateError(String.join("; ", deliveryErrors));
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() <= MAX_ERROR_LENGTH
                ? errorMessage
                : errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}
