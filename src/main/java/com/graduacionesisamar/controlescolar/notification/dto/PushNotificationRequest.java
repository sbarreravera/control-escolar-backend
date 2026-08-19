package com.graduacionesisamar.controlescolar.notification.dto;

import java.util.Map;

/**
 * Contains the information required to send a push notification.
 */
public record PushNotificationRequest(

        String token,

        String title,

        String body,

        Map<String, String> data

) {

    public PushNotificationRequest {
        data = data == null
                ? Map.of()
                : Map.copyOf(data);
    }
}