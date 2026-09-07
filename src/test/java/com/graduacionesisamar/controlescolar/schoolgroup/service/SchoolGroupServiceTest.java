package com.graduacionesisamar.controlescolar.schoolgroup.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.CreateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.SchoolGroupResponse;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolGroupServiceTest {

    @Mock
    private SchoolGroupRepository schoolGroupRepository;

    @Mock
    private AcademicCycleRepository academicCycleRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    private SchoolGroupService schoolGroupService;
    private AcademicCycle cycle;

    @BeforeEach
    void setUp() {
        schoolGroupService = new SchoolGroupService(
                schoolGroupRepository,
                academicCycleRepository,
                schoolAccessService
        );

        School school = new School();
        school.setId(10L);
        school.setName("Colegio Ejemplo");

        cycle = new AcademicCycle();
        cycle.setId(20L);
        cycle.setSchool(school);
        cycle.setName("2026-2027");
    }

    @Test
    void createRegistersNormalizedGroup() {
        CreateSchoolGroupRequest request = new CreateSchoolGroupRequest(
                20L,
                "  Primer grado  ",
                " a "
        );

        when(academicCycleRepository.findById(20L))
                .thenReturn(Optional.of(cycle));
        when(schoolGroupRepository.save(any(SchoolGroup.class)))
                .thenAnswer(invocation -> {
                    SchoolGroup group = invocation.getArgument(0);
                    group.setId(30L);
                    group.beforeInsert();
                    return group;
                });

        SchoolGroupResponse response = schoolGroupService.create(request);

        assertEquals(30L, response.id());
        assertEquals(10L, response.schoolId());
        assertEquals(20L, response.academicCycleId());
        assertEquals("Primer grado", response.gradeName());
        assertEquals("A", response.groupName());
        assertTrue(response.active());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void createRejectsDuplicatedGradeAndGroupWithinCycle() {
        CreateSchoolGroupRequest request = new CreateSchoolGroupRequest(
                20L,
                "Primer grado",
                "A"
        );

        when(academicCycleRepository.findById(20L))
                .thenReturn(Optional.of(cycle));
        when(schoolGroupRepository
                .existsByAcademicCycle_IdAndGradeNameIgnoreCaseAndGroupNameIgnoreCase(
                        20L,
                        "Primer grado",
                        "A"
                )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolGroupService.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(schoolGroupRepository, never()).save(any());
    }
}
