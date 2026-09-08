package com.graduacionesisamar.controlescolar.notification.job;

import com.graduacionesisamar.controlescolar.notification.service.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically processes pending guardian notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {
                "app.firebase.enabled",
                "app.notifications.delivery-enabled"
        },
        havingValue = "true"
)
public class NotificationDeliveryJob {

    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * Processes pending notifications after each configured delay.
     */
    @Scheduled(
            initialDelayString =
                    "${app.notifications.delivery-delay-ms:5000}",
            fixedDelayString =
                    "${app.notifications.delivery-delay-ms:5000}"
    )
    public void deliverPendingNotifications() {
        int delivered =
                notificationDeliveryService.deliverPending();

        if (delivered > 0) {
            log.info(
                    "Delivered {} pending notification(s)",
                    delivered
            );
        }
    }
}