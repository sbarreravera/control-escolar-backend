package com.graduacionesisamar.controlescolar.notification.service;

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
@ConditionalOnProperty(
        name = "app.firebase.enabled",
        havingValue = "true"
)
public class NotificationDeliveryService {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final NotificationLogRepository notificationLogRepository;
    private final GuardianDeviceRepository guardianDeviceRepository;
    private final PushNotificationClient pushNotificationClient;

    /**
     * Processes up to 50 pending notifications.
     *
     * @return number of notifications delivered to at least one device
     */
    public int deliverPending() {
        List<NotificationLog> pendingNotifications =
                notificationLogRepository
                        .findTop50ByStatusOrderByCreatedAtAsc(
                                NotificationStatus.PENDING
                        );

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
            markAsFailed(
                    notification,
                    "Guardian has no active registered devices"
            );
            return false;
        }

        int successfulDeliveries = 0;
        List<String> deliveryErrors = new ArrayList<>();
        OffsetDateTime deliveryTime = OffsetDateTime.now();

        for (GuardianDevice device : devices) {
            try {
                pushNotificationClient.send(
                        buildRequest(notification, device)
                );

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
            markAsSent(
                    notification,
                    deliveryTime,
                    deliveryErrors.size()
            );
            return true;
        }

        markAsFailed(
                notification,
                buildFailureMessage(deliveryErrors)
        );
        return false;
    }

    private PushNotificationRequest buildRequest(
            NotificationLog notification,
            GuardianDevice device
    ) {
        return new PushNotificationRequest(
                device.getFcmToken(),
                notification.getTitle(),
                notification.getMessage(),
                Map.of(
                        "notificationLogId",
                        String.valueOf(notification.getId()),
                        "accessEventId",
                        String.valueOf(
                                notification.getAccessEvent().getId()
                        ),
                        "studentId",
                        String.valueOf(
                                notification.getAccessEvent()
                                        .getStudent()
                                        .getId()
                        ),
                        "eventType",
                        notification.getAccessEvent()
                                .getEventType()
                                .name(),
                        "occurredAt",
                        notification.getAccessEvent()
                                .getOccurredAt()
                                .toString(),
                        "route",
                        "/#/guardian?eventId="
                                + notification.getAccessEvent().getId()
                )
        );
    }

    private void markAsSent(
            NotificationLog notification,
            OffsetDateTime sentAt,
            int failedDevices
    ) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(sentAt);

        if (failedDevices == 0) {
            notification.setErrorMessage(null);
        } else {
            notification.setErrorMessage(
                    "%d device delivery attempt(s) failed"
                            .formatted(failedDevices)
            );
        }

        notificationLogRepository.save(notification);
    }

    private void markAsFailed(
            NotificationLog notification,
            String errorMessage
    ) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setSentAt(null);
        notification.setErrorMessage(
                truncateError(errorMessage)
        );

        notificationLogRepository.save(notification);
    }

    private String buildFailureMessage(
            List<String> deliveryErrors
    ) {
        if (deliveryErrors.isEmpty()) {
            return "Notification could not be delivered";
        }

        return truncateError(
                String.join("; ", deliveryErrors)
        );
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }

        if (errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }

        return errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}
