package com.graduacionesisamar.controlescolar.firebase.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.graduacionesisamar.controlescolar.notification.client.PushNotificationClient;
import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;
import com.graduacionesisamar.controlescolar.notification.exception.PushNotificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> data = new HashMap<>(request.data());
        data.put("title", request.title());
        data.put("body", request.body());

        Message message = Message.builder()
                .setToken(request.token())
                .putAllData(data)
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
