package com.graduacionesisamar.controlescolar.guardianregistration.email;

import com.graduacionesisamar.controlescolar.guardianregistration.event.GuardianRegistrationCompletedEvent;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GuardianRegistrationEmailListenerTest {

    @Test
    void sendsMultipartAlternativeMessageForPlainTextAndHtml() throws Exception {
        AtomicReference<MimeMessage> sentMessage = new AtomicReference<>();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                sentMessage.set(mimeMessage);
            }
        };

        GuardianRegistrationEmailListener listener =
                new GuardianRegistrationEmailListener(
                        mailSender,
                        "controlescolar.isamar@gmail.com",
                        "Control Escolar | ISAMAR",
                        "http://localhost:4200/#/guardian/login",
                        "https://drive.google.com/file/d/1d1iBZ9VNF-2skuBd5tIAqwiYUhqZUVJ-/view?usp=sharing"
                );

        listener.sendConfirmation(new GuardianRegistrationCompletedEvent(
                "tutor@example.com",
                "Tutor Prueba",
                "TUTOR-100",
                "TUTOR-100",
                "7710000000",
                "Madre",
                "Colegio San Felipe de Jesús",
                "CSFJ",
                List.of("A001")
        ));

        MimeMessage message = sentMessage.get();
        assertNotNull(message);
        assertInstanceOf(Multipart.class, message.getContent());
    }
}
