package com.graduacionesisamar.controlescolar.schoolgroup.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.CreateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.SchoolGroupResponse;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.UpdateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private StudentRepository studentRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    private SchoolGroupService schoolGroupService;
    private AcademicCycle cycle;

    @BeforeEach
    void setUp() {
        schoolGroupService = new SchoolGroupService(
                schoolGroupRepository,
                academicCycleRepository,
                studentRepository,
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
        assertFalse(response.hasStudents());
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

    @Test
    void updateCorrectsNamesWithoutChangingAcademicCycle() {
        SchoolGroup group = existingGroup();
        UpdateSchoolGroupRequest request = new UpdateSchoolGroupRequest(
                "  Segundo semestre ",
                " gpo 2 "
        );

        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(schoolGroupRepository.save(group)).thenReturn(group);

        SchoolGroupResponse response = schoolGroupService.update(30L, request);

        assertEquals("Segundo semestre", response.gradeName());
        assertEquals("GPO 2", response.groupName());
        assertEquals(20L, response.academicCycleId());
        assertEquals(cycle, group.getAcademicCycle());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void updateRejectsDuplicatedGradeAndGroupWithinCycle() {
        SchoolGroup group = existingGroup();
        UpdateSchoolGroupRequest request = new UpdateSchoolGroupRequest(
                "Primer grado",
                "B"
        );

        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(schoolGroupRepository
                .existsByAcademicCycle_IdAndGradeNameIgnoreCaseAndGroupNameIgnoreCaseAndIdNot(
                        20L,
                        "Primer grado",
                        "B",
                        30L
                )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolGroupService.update(30L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(schoolGroupRepository, never()).save(any());
    }

    @Test
    void deleteRejectsGroupWithStudents() {
        SchoolGroup group = existingGroup();

        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(studentRepository.existsBySchoolGroup_Id(30L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolGroupService.delete(30L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(schoolGroupRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesEmptyGroup() {
        SchoolGroup group = existingGroup();

        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(studentRepository.existsBySchoolGroup_Id(30L))
                .thenReturn(false);

        schoolGroupService.delete(30L);

        verify(schoolAccessService).requireAccessToSchool(10L);
        verify(schoolGroupRepository).delete(group);
    }

    private SchoolGroup existingGroup() {
        SchoolGroup group = new SchoolGroup();
        group.setId(30L);
        group.setAcademicCycle(cycle);
        group.setGradeName("Primer grado");
        group.setGroupName("A");
        group.beforeInsert();
        return group;
    }
}
