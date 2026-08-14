package com.graduacionesisamar.controlescolar.accessevent.service;

import com.graduacionesisamar.controlescolar.accessevent.dto.AccessEventResponse;
import com.graduacionesisamar.controlescolar.accessevent.dto.ScanAccessEventRequest;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;
import com.graduacionesisamar.controlescolar.accessevent.repository.AccessEventRepository;
import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.credential.repository.CredentialRepository;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the business rules for student access events.
 */
@ExtendWith(MockitoExtension.class)
class AccessEventServiceTest {

    private static final String QR_TOKEN = "valid-qr-token";

    @Mock
    private AccessEventRepository accessEventRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private AccessEventService accessEventService;

    private Student student;
    private Credential credential;

    @BeforeEach
    void setUp() {
        student = buildStudent();
        credential = buildCredential();
    }

    @Test
    void scanRegistersEntryFromActiveCredential() {
        ScanAccessEventRequest request = buildRequest(
                AccessEventType.ENTRY,
                CaptureMethod.QR_CAMERA
        );

        when(credentialRepository.findByQrTokenAndActiveTrue(QR_TOKEN))
                .thenReturn(Optional.of(credential));
        stubSavedEvent(100L);

        AccessEventResponse response = accessEventService.scan(request);

        assertEquals(100L, response.id());
        assertEquals(1L, response.studentId());
        assertEquals("Samuel Barrera Vera", response.studentName());
        assertEquals("MAT-001", response.enrollmentNumber());
        assertEquals(10L, response.credentialId());
        assertEquals(AccessEventType.ENTRY, response.eventType());
        assertEquals(CaptureMethod.QR_CAMERA, response.captureMethod());
        assertEquals("Entrada principal", response.deviceName());
        assertNotNull(response.occurredAt());
    }

    @Test
    void scanAcceptsUsbExitAndTrimsValues() {
        ScanAccessEventRequest request = new ScanAccessEventRequest(
                "  " + QR_TOKEN + "  ",
                AccessEventType.EXIT,
                CaptureMethod.QR_USB,
                "  Salida principal  ",
                "   "
        );

        when(credentialRepository.findByQrTokenAndActiveTrue(QR_TOKEN))
                .thenReturn(Optional.of(credential));
        stubSavedEvent(101L);

        AccessEventResponse response = accessEventService.scan(request);

        assertEquals(AccessEventType.EXIT, response.eventType());
        assertEquals(CaptureMethod.QR_USB, response.captureMethod());
        assertEquals("Salida principal", response.deviceName());
        assertEquals(null, response.notes());
    }

    @Test
    void scanRejectsUnknownCredential() {
        ScanAccessEventRequest request = buildRequest(
                AccessEventType.ENTRY,
                CaptureMethod.QR_CAMERA
        );

        when(credentialRepository.findByQrTokenAndActiveTrue(QR_TOKEN))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessEventService.scan(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(accessEventRepository, never()).save(any());
    }

    @Test
    void scanRejectsManualCapture() {
        ScanAccessEventRequest request = buildRequest(
                AccessEventType.ENTRY,
                CaptureMethod.MANUAL
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessEventService.scan(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(credentialRepository);
        verifyNoInteractions(accessEventRepository);
    }

    @Test
    void scanRejectsExpiredCredential() {
        credential.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        ScanAccessEventRequest request = buildRequest(
                AccessEventType.ENTRY,
                CaptureMethod.QR_CAMERA
        );

        when(credentialRepository.findByQrTokenAndActiveTrue(QR_TOKEN))
                .thenReturn(Optional.of(credential));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessEventService.scan(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(accessEventRepository, never()).save(any());
    }

    @Test
    void scanRejectsInactiveStudent() {
        student.setActive(false);

        ScanAccessEventRequest request = buildRequest(
                AccessEventType.ENTRY,
                CaptureMethod.QR_CAMERA
        );

        when(credentialRepository.findByQrTokenAndActiveTrue(QR_TOKEN))
                .thenReturn(Optional.of(credential));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessEventService.scan(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(accessEventRepository, never()).save(any());
    }

    private Student buildStudent() {
        Student value = new Student();
        value.setId(1L);
        value.setEnrollmentNumber("MAT-001");
        value.setFirstName("Samuel");
        value.setLastName("Barrera Vera");
        value.setActive(true);
        return value;
    }

    private Credential buildCredential() {
        Credential value = new Credential();
        value.setId(10L);
        value.setStudent(student);
        value.setQrToken(QR_TOKEN);
        value.setActive(true);
        value.beforeInsert();
        return value;
    }

    private ScanAccessEventRequest buildRequest(
            AccessEventType eventType,
            CaptureMethod captureMethod
    ) {
        return new ScanAccessEventRequest(
                QR_TOKEN,
                eventType,
                captureMethod,
                "Entrada principal",
                "Registro de prueba"
        );
    }

    private void stubSavedEvent(Long eventId) {
        when(accessEventRepository.save(any(AccessEvent.class)))
                .thenAnswer(invocation -> {
                    AccessEvent event = invocation.getArgument(0);
                    event.setId(eventId);
                    event.beforeInsert();
                    return event;
                });
    }
}