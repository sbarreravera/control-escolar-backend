package com.graduacionesisamar.controlescolar.firebase.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.graduacionesisamar.controlescolar.notification.client.PushNotificationClient;
import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;
import com.graduacionesisamar.controlescolar.notification.exception.PushNotificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sends push notifications through Firebase Cloud Messaging.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebasePushNotificationClient
        implements PushNotificationClient {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public String send(PushNotificationRequest request) {
        Message message = Message.builder()
                .setToken(request.token())
                .setNotification(
                        Notification.builder()
                                .setTitle(request.title())
                                .setBody(request.body())
                                .build()
                )
                .putAllData(request.data())
                .build();

        try {
            return firebaseMessaging.send(message);
        } catch (FirebaseMessagingException exception) {
            boolean invalidToken =
                    exception.getMessagingErrorCode()
                            == MessagingErrorCode.UNREGISTERED;

            throw new PushNotificationException(
                    "Unable to send Firebase notification",
                    invalidToken,
                    exception
            );
        }
    }
}