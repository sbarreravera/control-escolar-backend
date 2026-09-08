package com.graduacionesisamar.controlescolar.academiccycle.service;

import com.graduacionesisamar.controlescolar.academiccycle.dto.AcademicCycleResponse;
import com.graduacionesisamar.controlescolar.academiccycle.dto.CreateAcademicCycleRequest;
import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCycleServiceTest {

    @Mock
    private AcademicCycleRepository academicCycleRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    private AcademicCycleService academicCycleService;
    private School school;

    @BeforeEach
    void setUp() {
        academicCycleService = new AcademicCycleService(
                academicCycleRepository,
                schoolRepository,
                schoolAccessService
        );

        school = new School();
        school.setId(10L);
        school.setName("Colegio Ejemplo");
    }

    @Test
    void createRegistersNormalizedAcademicCycle() {
        CreateAcademicCycleRequest request = new CreateAcademicCycleRequest(
                10L,
                "  2026 - 2027  ",
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2027, 7, 9)
        );

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(academicCycleRepository.save(any(AcademicCycle.class)))
                .thenAnswer(invocation -> {
                    AcademicCycle cycle = invocation.getArgument(0);
                    cycle.setId(20L);
                    cycle.beforeInsert();
                    return cycle;
                });

        AcademicCycleResponse response = academicCycleService.create(request);

        assertEquals(20L, response.id());
        assertEquals(10L, response.schoolId());
        assertEquals("2026 - 2027", response.name());
        assertTrue(response.active());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        CreateAcademicCycleRequest request = new CreateAcademicCycleRequest(
                10L,
                "2026-2027",
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 23)
        );

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> academicCycleService.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(academicCycleRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicatedNameWithinSchool() {
        CreateAcademicCycleRequest request = new CreateAcademicCycleRequest(
                10L,
                "2026-2027",
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2027, 7, 9)
        );

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(academicCycleRepository
                .existsBySchool_IdAndNameIgnoreCase(10L, "2026-2027"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> academicCycleService.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(academicCycleRepository, never()).save(any());
    }

    @Test
    void findAllReturnsSchoolCyclesOrderedByRepository() {
        AcademicCycle cycle = new AcademicCycle();
        cycle.setId(20L);
        cycle.setSchool(school);
        cycle.setName("2026-2027");
        cycle.setStartDate(LocalDate.of(2026, 8, 24));
        cycle.setEndDate(LocalDate.of(2027, 7, 9));
        cycle.beforeInsert();

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(academicCycleRepository
                .findAllBySchool_IdOrderByStartDateDesc(10L))
                .thenReturn(List.of(cycle));

        List<AcademicCycleResponse> response =
                academicCycleService.findAllBySchool(10L);

        assertEquals(1, response.size());
        assertEquals("2026-2027", response.getFirst().name());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }
}
