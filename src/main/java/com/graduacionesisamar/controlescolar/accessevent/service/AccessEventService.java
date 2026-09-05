package com.graduacionesisamar.controlescolar.accessevent.service;

import com.graduacionesisamar.controlescolar.accessevent.dto.AccessEventResponse;
import com.graduacionesisamar.controlescolar.accessevent.dto.ScanAccessEventRequest;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;
import com.graduacionesisamar.controlescolar.accessevent.repository.AccessEventRepository;
import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.credential.repository.CredentialRepository;
import com.graduacionesisamar.controlescolar.notification.service.NotificationLogService;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AccessEventService {

    private final AccessEventRepository accessEventRepository;
    private final CredentialRepository credentialRepository;
    private final NotificationLogService notificationLogService;
    private final SchoolAccessService schoolAccessService;

    public AccessEventResponse scan(ScanAccessEventRequest request) {
        validateCaptureMethod(request.captureMethod());

        Credential credential = findCredential(request.qrToken().trim());
        Student student = credential.getStudent();

        schoolAccessService.requireAccessToSchool(
                student.getSchool().getId()
        );

        validateCredentialExpiration(credential);
        validateActiveStudent(student);

        AccessEvent event = buildEvent(request, credential, student);
        AccessEvent savedEvent = accessEventRepository.save(event);

        int notificationsQueued =
                notificationLogService.queueForEvent(savedEvent);

        return toResponse(savedEvent, notificationsQueued);
    }

    private Credential findCredential(String qrToken) {
        return credentialRepository
                .findByQrTokenAndActiveTrue(qrToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active credential not found"
                ));
    }

    private void validateCaptureMethod(CaptureMethod captureMethod) {
        if (captureMethod == CaptureMethod.MANUAL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Manual capture is not allowed in the scan endpoint"
            );
        }
    }

    private void validateCredentialExpiration(Credential credential) {
        OffsetDateTime expiresAt = credential.getExpiresAt();

        if (expiresAt != null && !expiresAt.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Credential has expired"
            );
        }
    }

    private void validateActiveStudent(Student student) {
        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student is inactive"
            );
        }
    }

    private AccessEvent buildEvent(
            ScanAccessEventRequest request,
            Credential credential,
            Student student
    ) {
        AccessEvent event = new AccessEvent();
        event.setStudent(student);
        event.setCredential(credential);
        event.setEventType(request.eventType());
        event.setCaptureMethod(request.captureMethod());
        event.setDeviceName(trimNullable(request.deviceName()));
        event.setNotes(trimNullable(request.notes()));
        return event;
    }

    private String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private AccessEventResponse toResponse(
            AccessEvent event,
            int notificationsQueued
    ) {
        Student student = event.getStudent();

        return new AccessEventResponse(
                event.getId(),
                student.getId(),
                buildStudentName(student),
                student.getEnrollmentNumber(),
                event.getCredential().getId(),
                event.getEventType(),
                event.getCaptureMethod(),
                event.getOccurredAt(),
                event.getDeviceName(),
                event.getNotes(),
                notificationsQueued
        );
    }

    private String buildStudentName(Student student) {
        return "%s %s".formatted(
                student.getFirstName(),
                student.getLastName()
        );
    }
}