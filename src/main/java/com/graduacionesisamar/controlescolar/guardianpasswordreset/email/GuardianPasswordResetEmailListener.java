package com.graduacionesisamar.controlescolar.guardianpasswordreset.email;

import com.graduacionesisamar.controlescolar.guardianpasswordreset.event.GuardianPasswordResetRequestedEvent;
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
 * Sends one-time guardian password reset links.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.mail",
        name = "enabled",
        havingValue = "true"
)
public class GuardianPasswordResetEmailListener {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String passwordResetUrl;

    public GuardianPasswordResetEmailListener(
            JavaMailSender mailSender,
            @Value("${app.mail.from-address}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName,
            @Value("${app.mail.portal-password-reset-url}")
            String passwordResetUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.passwordResetUrl = passwordResetUrl;
    }

    @EventListener
    public void sendResetLink(GuardianPasswordResetRequestedEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(event.recipientEmail());
            helper.setSubject(
                    "Recupera tu contraseña - " + event.schoolName()
            );
            helper.setText(
                    buildPlainText(event),
                    buildHtml(event)
            );

            mailSender.send(message);
            log.info(
                    "Guardian password reset email sent to {} for school {}",
                    event.recipientEmail(),
                    event.schoolCode()
            );
        } catch (Exception exception) {
            log.error(
                    "Guardian password reset email could not be sent to {}",
                    event.recipientEmail(),
                    exception
            );
        }
    }

    private String buildPlainText(
            GuardianPasswordResetRequestedEvent event
    ) {
        return """
                Hola, %s:

                Recibimos una solicitud para cambiar la contraseña de tu cuenta de tutor en %s.

                Código de escuela: %s
                Usuario: %s

                Crea una nueva contraseña desde este enlace:
                %s

                El enlace es de un solo uso y vence una hora después de la solicitud.
                Al cambiar la contraseña se cerrarán las sesiones anteriores por seguridad.

                Si no solicitaste este cambio, puedes ignorar este mensaje. Tu contraseña actual seguirá funcionando mientras el enlace no sea utilizado.

                Powered by Estudio Digital ISAMAR
                """.formatted(
                event.guardianName(),
                event.schoolName(),
                event.schoolCode(),
                event.username(),
                buildResetUrl(event)
        );
    }

    private String buildHtml(
            GuardianPasswordResetRequestedEvent event
    ) {
        String resetUrl = escape(buildResetUrl(event));

        return """
                <!doctype html>
                <html lang="es">
                  <body style="margin:0;padding:24px;background:#f4f6f9;font-family:Arial,sans-serif;color:#1f2937;">
                    <div style="max-width:640px;margin:0 auto;background:#ffffff;border-radius:12px;padding:28px;border:1px solid #e5e7eb;">
                      <p style="margin:0 0 8px;color:#4f46e5;font-size:13px;font-weight:700;letter-spacing:.04em;">CONTROL ESCOLAR | ISAMAR</p>
                      <h1 style="margin:0 0 16px;font-size:24px;">Recupera tu contraseña</h1>
                      <p>Hola, <strong>%s</strong>.</p>
                      <p>Recibimos una solicitud para cambiar la contraseña de tu cuenta de tutor en <strong>%s</strong>.</p>

                      <div style="margin:24px 0;padding:18px;background:#eef2ff;border:1px solid #c7d2fe;border-radius:10px;">
                        <p style="margin:0 0 12px;color:#3730a3;font-size:14px;font-weight:700;">DATOS DE ACCESO</p>
                        <p style="margin:0 0 8px;"><strong>Código de escuela:</strong> %s</p>
                        <p style="margin:0;"><strong>Usuario:</strong> %s</p>
                      </div>

                      <p style="margin:28px 0;">
                        <a href="%s" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:8px;font-weight:700;">Crear nueva contraseña</a>
                      </p>

                      <p style="font-size:13px;color:#64748b;">El enlace es de un solo uso y vence una hora después de la solicitud. Al cambiar la contraseña se cerrarán las sesiones anteriores por seguridad.</p>
                      <p style="font-size:13px;color:#64748b;">Si no solicitaste este cambio, puedes ignorar este mensaje. Tu contraseña actual seguirá funcionando mientras el enlace no sea utilizado.</p>

                      <div style="margin-top:30px;padding-top:20px;border-top:1px solid #e5e7eb;text-align:center;">
                        <p style="margin:0 0 3px;color:#94a3b8;font-size:11px;letter-spacing:.08em;text-transform:uppercase;">Powered by</p>
                        <p style="margin:0;color:#475569;font-size:13px;">Estudio Digital <strong style="color:#312e81;letter-spacing:.08em;">ISAMAR</strong></p>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                escape(event.guardianName()),
                escape(event.schoolName()),
                escape(event.schoolCode()),
                escape(event.username()),
                resetUrl
        );
    }

    private String buildResetUrl(
            GuardianPasswordResetRequestedEvent event
    ) {
        String separator = passwordResetUrl.contains("?") ? "&" : "?";
        return passwordResetUrl
                + separator
                + "token="
                + URLEncoder.encode(
                        event.resetToken(),
                        StandardCharsets.UTF_8
                );
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
