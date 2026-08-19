package com.graduacionesisamar.controlescolar.notification.exception;

/**
 * Represents an error returned by a push notification provider.
 */
public class PushNotificationException extends RuntimeException {

    private final boolean invalidToken;

    public PushNotificationException(
            String message,
            boolean invalidToken,
            Throwable cause
    ) {
        super(message, cause);
        this.invalidToken = invalidToken;
    }

    /**
     * Indicates whether the destination token is no longer valid.
     */
    public boolean isInvalidToken() {
        return invalidToken;
    }
}