package com.graduacionesisamar.controlescolar.accessevent.service;

import com.graduacionesisamar.controlescolar.accessevent.controller.AccessMonitorController;
import com.graduacionesisamar.controlescolar.accessevent.dto.AccessMonitorPageResponse;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.entity.CaptureMethod;
import com.graduacionesisamar.controlescolar.accessevent.repository.AccessEventRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.security.module.SchoolModule;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessMonitorServiceTest {

    @Mock AccessEventRepository accessEventRepository;
    @Mock SchoolAccessService schoolAccessService;
    @InjectMocks AccessMonitorService service;

    @Test
    void usesMexicoCityLocalDayWithExclusiveNextMidnight() {
        var window = AccessMonitorService.windowFor(LocalDate.of(2026, 11, 6));
        assertEquals(
                OffsetDateTime.of(2026, 11, 6, 0, 0, 0, 0, ZoneOffset.ofHours(-6)),
                window.start()
        );
        assertEquals(
                OffsetDateTime.of(2026, 11, 7, 0, 0, 0, 0, ZoneOffset.ofHours(-6)),
                window.end()
        );
    }

    @Test
    void returnsReadOnlyPaginatedEventsScopedToRequestedSchool() {
        Student student = new Student();
        student.setId(18L);
        student.setFirstName("Ana");
        student.setLastName("Pérez");
        student.setEnrollmentNumber("261018");
        student.setGradeName("1");
        student.setGroupName("A");
        School school = new School();
        school.setId(44L);
        student.setSchool(school);

        AccessEvent accessEvent = new AccessEvent();
        accessEvent.setId(20L);
        accessEvent.setStudent(student);
        accessEvent.setEventType(AccessEventType.ENTRY);
        accessEvent.setCaptureMethod(CaptureMethod.QR_USB);
        accessEvent.setOccurredAt(OffsetDateTime.parse("2026-11-06T07:35:00-06:00"));
        accessEvent.setDeviceName("Entrada 1");

        when(accessEventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(accessEvent)));

        AccessMonitorPageResponse result = service.findPage(
                44L,
                LocalDate.of(2026, 11, 6),
                0,
                25,
                "Ana",
                null,
                null,
                AccessEventType.ENTRY
        );

        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals("Ana Pérez", result.content().getFirst().studentName());
        assertEquals("261018", result.content().getFirst().enrollmentNumber());
        assertEquals("1", result.content().getFirst().gradeName());
        assertEquals(AccessEventType.ENTRY, result.content().getFirst().eventType());
        assertEquals("Entrada 1", result.content().getFirst().deviceName());

        verify(schoolAccessService).requireAccessToSchool(44L);
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(accessEventRepository).findAll(any(Specification.class), pageCaptor.capture());
        assertEquals(0, pageCaptor.getValue().getPageNumber());
        assertEquals(25, pageCaptor.getValue().getPageSize());
        assertEquals("occurredAt: DESC,id: DESC", pageCaptor.getValue().getSort().toString());
        verifyNoMoreInteractions(accessEventRepository);
    }

    @Test
    void deniesAccessToAnotherSchoolBeforeQueryingDatabase() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(schoolAccessService).requireAccessToSchool(999L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.findPage(
                        999L,
                        LocalDate.of(2026, 11, 6),
                        0, 25, "", null, null, null
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(accessEventRepository);
    }

    @Test
    void controllerRequiresSeparateAccessMonitorModule() {
        SchoolModule access = AccessMonitorController.class.getAnnotation(SchoolModule.class);
        assertNotNull(access);
        assertEquals("ACCESS_MONITOR", access.key());
        assertFalse(access.defaultGranted());
    }
}
