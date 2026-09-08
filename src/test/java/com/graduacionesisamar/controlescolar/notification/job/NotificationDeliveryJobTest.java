package com.graduacionesisamar.controlescolar.notification.job;

import com.graduacionesisamar.controlescolar.notification.service.NotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryJobTest {

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    @InjectMocks
    private NotificationDeliveryJob notificationDeliveryJob;

    @Test
    void deliverPendingNotificationsDelegatesToService() {
        when(notificationDeliveryService.deliverPending())
                .thenReturn(2);

        assertDoesNotThrow(
                notificationDeliveryJob
                        ::deliverPendingNotifications
        );

        verify(notificationDeliveryService)
                .deliverPending();
    }
}