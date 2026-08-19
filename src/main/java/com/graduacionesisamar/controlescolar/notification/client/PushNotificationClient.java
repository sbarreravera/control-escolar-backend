package com.graduacionesisamar.controlescolar.notification.client;

import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;

/**
 * Defines the contract for sending push notifications.
 */
public interface PushNotificationClient {

    /**
     * Sends a push notification and returns the provider message ID.
     */
    String send(PushNotificationRequest request);
}