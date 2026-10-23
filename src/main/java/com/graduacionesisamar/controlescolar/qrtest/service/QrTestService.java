package com.graduacionesisamar.controlescolar.qrtest.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.credential.repository.CredentialRepository;
import com.graduacionesisamar.controlescolar.qrtest.dto.QrTestRequest;
import com.graduacionesisamar.controlescolar.qrtest.dto.QrTestResponse;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Ruta aislada del flujo real de accesos y notificaciones.
 * No inyecta AccessEventService ni NotificationLogService.
 */
@Service
@RequiredArgsConstructor
public class QrTestService {

    private final CredentialRepository credentialRepository;
    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public QrTestResponse validate(QrTestRequest request) {
        AppUserRole role = schoolAccessService.getCurrentUser().getRole();
        if (role != AppUserRole.ADMIN && role != AppUserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }

        schoolAccessService.requireAccessToSchool(request.schoolId());

        Credential credential = credentialRepository
                .findByQrToken(request.qrToken().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "QR credential not found"));

        Student student = credential.getStudent();
        if (!Objects.equals(student.getSchool().getId(), request.schoolId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "QR credential not found");
        }

        String status;
        if (!Boolean.TRUE.equals(credential.getActive())) {
            status = "INACTIVE_CREDENTIAL";
        } else if (credential.getExpiresAt() != null
                && !credential.getExpiresAt().isAfter(OffsetDateTime.now())) {
            status = "EXPIRED";
        } else if (!Boolean.TRUE.equals(student.getActive())) {
            status = "INACTIVE_STUDENT";
        } else {
            status = "VALID";
        }

        String grade = student.getSchoolGroup() != null
                ? student.getSchoolGroup().getGradeName() : student.getGradeName();
        String group = student.getSchoolGroup() != null
                ? student.getSchoolGroup().getGroupName() : student.getGroupName();

        return new QrTestResponse(
                "VALID".equals(status),
                status,
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEnrollmentNumber(),
                student.getSchool().getName(),
                grade,
                group,
                credential.getExpiresAt()
        );
    }
}
