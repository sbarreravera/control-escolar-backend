package com.graduacionesisamar.controlescolar.guardianregistration.email;

import com.graduacionesisamar.controlescolar.guardianregistration.event.GuardianRegistrationCompletedEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends guardian registration confirmations after the registration service
 * returns successfully. The controller publishes the event only after the
 * transactional service call has committed. Mail delivery failures are logged
 * and never affect the already-created guardian account.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.mail",
        name = "enabled",
        havingValue = "true"
)
public class GuardianRegistrationEmailListener {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String portalLoginUrl;

    public GuardianRegistrationEmailListener(
            JavaMailSender mailSender,
            @Value("${app.mail.from-address}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName,
            @Value("${app.mail.portal-login-url}") String portalLoginUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.portalLoginUrl = portalLoginUrl;
    }

    @EventListener
    public void sendConfirmation(
            GuardianRegistrationCompletedEvent event
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(event.recipientEmail());
            helper.setSubject("Registro confirmado - " + event.schoolName());
            helper.setText(
                    buildPlainText(event),
                    buildHtml(event)
            );

            mailSender.send(message);
            log.info(
                    "Guardian registration confirmation sent to {} for school {}",
                    event.recipientEmail(),
                    event.schoolCode()
            );
        } catch (Exception exception) {
            log.error(
                    "Guardian registration was created, but confirmation email could not be sent to {}",
                    event.recipientEmail(),
                    exception
            );
        }
    }

    private String buildPlainText(
            GuardianRegistrationCompletedEvent event
    ) {
        StringBuilder students = new StringBuilder();
        for (String enrollmentNumber : event.studentEnrollmentNumbers()) {
            students.append("- Matrícula ")
                    .append(enrollmentNumber)
                    .append(System.lineSeparator());
        }

        return """
                Hola, %s:

                Tu registro como tutor en %s fue completado correctamente.

                Datos registrados
                Nombre: %s
                Correo: %s
                Teléfono: %s
                Parentesco: %s
                Matrícula de tutor / usuario: %s

                Alumnos asociados
                %s
                Acceso al portal:
                %s

                Por seguridad, tu contraseña no se incluye en este correo.
                Si no realizaste este registro, comunícate con la institución.
                """.formatted(
                event.guardianName(),
                event.schoolName(),
                event.guardianName(),
                event.recipientEmail(),
                hasText(event.phone()) ? event.phone() : "No capturado",
                event.relationship(),
                event.username(),
                students.toString().stripTrailing(),
                buildLoginUrl(event)
        );
    }

    private String buildHtml(
            GuardianRegistrationCompletedEvent event
    ) {
        StringBuilder students = new StringBuilder();
        for (String enrollmentNumber : event.studentEnrollmentNumbers()) {
            students.append("<li>Matrícula <strong>")
                    .append(escape(enrollmentNumber))
                    .append("</strong></li>");
        }

        String phone = hasText(event.phone())
                ? escape(event.phone())
                : "No capturado";
        String loginUrl = escape(buildLoginUrl(event));

        return """
                <!doctype html>
                <html lang="es">
                  <body style="margin:0;padding:24px;background:#f4f6f9;font-family:Arial,sans-serif;color:#1f2937;">
                    <div style="max-width:640px;margin:0 auto;background:#ffffff;border-radius:12px;padding:28px;border:1px solid #e5e7eb;">
                      <p style="margin:0 0 8px;color:#4f46e5;font-size:13px;font-weight:700;letter-spacing:.04em;">CONTROL ESCOLAR | ISAMAR</p>
                      <h1 style="margin:0 0 16px;font-size:24px;">Registro confirmado</h1>
                      <p>Hola, <strong>%s</strong>.</p>
                      <p>Tu registro como tutor en <strong>%s</strong> fue completado correctamente.</p>

                      <div style="margin:24px 0;padding:18px;background:#f8fafc;border-radius:10px;">
                        <p style="margin:0 0 8px;"><strong>Nombre:</strong> %s</p>
                        <p style="margin:0 0 8px;"><strong>Correo:</strong> %s</p>
                        <p style="margin:0 0 8px;"><strong>Teléfono:</strong> %s</p>
                        <p style="margin:0 0 8px;"><strong>Parentesco:</strong> %s</p>
                        <p style="margin:0;"><strong>Matrícula de tutor / usuario:</strong> %s</p>
                      </div>

                      <h2 style="font-size:18px;margin-bottom:8px;">Alumnos asociados</h2>
                      <ul style="padding-left:20px;line-height:1.7;">%s</ul>

                      <p style="margin:28px 0;">
                        <a href="%s" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:8px;font-weight:700;">Entrar al portal de tutores</a>
                      </p>

                      <p style="font-size:13px;color:#64748b;">Por seguridad, tu contraseña no se incluye en este correo. Si no realizaste este registro, comunícate con la institución.</p>
                    </div>
                  </body>
                </html>
                """.formatted(
                escape(event.guardianName()),
                escape(event.schoolName()),
                escape(event.guardianName()),
                escape(event.recipientEmail()),
                phone,
                escape(event.relationship()),
                escape(event.username()),
                students,
                loginUrl
        );
    }

    private String buildLoginUrl(
            GuardianRegistrationCompletedEvent event
    ) {
        String separator = portalLoginUrl.contains("?") ? "&" : "?";
        return portalLoginUrl
                + separator
                + "schoolCode="
                + encode(event.schoolCode())
                + "&username="
                + encode(event.username());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
