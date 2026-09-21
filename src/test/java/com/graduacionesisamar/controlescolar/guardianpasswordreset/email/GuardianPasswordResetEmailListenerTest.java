package com.graduacionesisamar.controlescolar.guardianpasswordreset.email;

import com.graduacionesisamar.controlescolar.guardianpasswordreset.event.GuardianPasswordResetRequestedEvent;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GuardianPasswordResetEmailListenerTest {

    @Test
    void sendsMultipartResetMessage() throws Exception {
        AtomicReference<MimeMessage> sentMessage = new AtomicReference<>();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                sentMessage.set(mimeMessage);
            }
        };

        GuardianPasswordResetEmailListener listener =
                new GuardianPasswordResetEmailListener(
                        mailSender,
                        "controlescolar.isamar@gmail.com",
                        "Control Escolar | ISAMAR",
                        "http://localhost:4200/#/guardian/reset-password"
                );

        listener.sendResetLink(new GuardianPasswordResetRequestedEvent(
                "tutor@example.com",
                "Tutor Prueba",
                "Colegio de Prueba",
                "ESC-001",
                "TUTOR-001",
                "reset-token",
                OffsetDateTime.now().plusHours(1)
        ));

        MimeMessage message = sentMessage.get();
        assertNotNull(message);
        assertInstanceOf(Multipart.class, message.getContent());
    }
}
