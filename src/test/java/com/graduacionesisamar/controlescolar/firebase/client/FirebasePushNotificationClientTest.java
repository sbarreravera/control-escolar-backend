package com.graduacionesisamar.controlescolar.firebase.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.graduacionesisamar.controlescolar.notification.dto.PushNotificationRequest;
import com.graduacionesisamar.controlescolar.notification.exception.PushNotificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebasePushNotificationClientTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private FirebasePushNotificationClient client;

    @Test
    void sendReturnsFirebaseMessageId()
            throws FirebaseMessagingException {
        PushNotificationRequest request =
                createRequest();

        when(firebaseMessaging.send(any(Message.class)))
                .thenReturn("firebase-message-123");

        String messageId = client.send(request);

        assertEquals("firebase-message-123", messageId);

        verify(firebaseMessaging)
                .send(any(Message.class));
    }

    @Test
    void sendIdentifiesUnregisteredToken()
            throws FirebaseMessagingException {
        PushNotificationRequest request =
                createRequest();

        FirebaseMessagingException firebaseException =
                mock(FirebaseMessagingException.class);

        when(firebaseException.getMessagingErrorCode())
                .thenReturn(MessagingErrorCode.UNREGISTERED);

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(firebaseException);

        PushNotificationException exception = assertThrows(
                PushNotificationException.class,
                () -> client.send(request)
        );

        assertTrue(exception.isInvalidToken());
        assertSame(firebaseException, exception.getCause());
        assertEquals(
                "Unable to send Firebase notification",
                exception.getMessage()
        );
    }

    @Test
    void sendPreservesNonTokenFailure()
            throws FirebaseMessagingException {
        PushNotificationRequest request =
                createRequest();

        FirebaseMessagingException firebaseException =
                mock(FirebaseMessagingException.class);

        when(firebaseException.getMessagingErrorCode())
                .thenReturn(MessagingErrorCode.INTERNAL);

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(firebaseException);

        PushNotificationException exception = assertThrows(
                PushNotificationException.class,
                () -> client.send(request)
        );

        assertFalse(exception.isInvalidToken());
        assertSame(firebaseException, exception.getCause());
    }

    private PushNotificationRequest createRequest() {
        return new PushNotificationRequest(
                "test-fcm-token",
                "Entrada registrada",
                "Samuel Barrera Vera registró una entrada.",
                Map.of(
                        "accessEventId", "1",
                        "studentId", "1",
                        "eventType", "ENTRY"
                )
        );
    }
}