package com.graduacionesisamar.controlescolar.student.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.dto.CreateStudentRequest;
import com.graduacionesisamar.controlescolar.student.dto.StudentResponse;
import com.graduacionesisamar.controlescolar.student.dto.StudentPageResponse;
import com.graduacionesisamar.controlescolar.student.dto.UpdateStudentRequest;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SchoolGroupRepository schoolGroupRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    private StudentService studentService;
    private School school;
    private SchoolGroup group;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentRepository,
                schoolRepository,
                schoolGroupRepository,
                schoolAccessService
        );

        school = new School();
        school.setId(10L);
        school.setName("Colegio Ejemplo");

        AcademicCycle cycle = new AcademicCycle();
        cycle.setId(20L);
        cycle.setSchool(school);
        cycle.setName("2026-2027");
        cycle.setActive(true);

        group = new SchoolGroup();
        group.setId(30L);
        group.setAcademicCycle(cycle);
        group.setGradeName("Primer grado");
        group.setGroupName("A");
        group.setActive(true);
    }

    @Test
    void createAssociatesStudentWithSelectedGroup() {
        CreateStudentRequest request = new CreateStudentRequest(
                10L,
                " mat-001 ",
                " Ana ",
                " Pérez ",
                null,
                null,
                30L
        );

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(studentRepository.save(any(Student.class)))
                .thenAnswer(invocation -> {
                    Student student = invocation.getArgument(0);
                    student.setId(40L);
                    student.beforeInsert();
                    return student;
                });

        StudentResponse response = studentService.create(request);

        assertEquals("MAT-001", response.enrollmentNumber());
        assertEquals("Primer grado", response.gradeName());
        assertEquals("A", response.groupName());
        assertEquals(30L, response.schoolGroupId());
        assertEquals(20L, response.academicCycleId());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void createRejectsGroupFromAnotherSchool() {
        School anotherSchool = new School();
        anotherSchool.setId(99L);

        group.getAcademicCycle().setSchool(anotherSchool);

        CreateStudentRequest request = new CreateStudentRequest(
                10L,
                "MAT-001",
                "Ana",
                "Pérez",
                null,
                null,
                30L
        );

        when(schoolRepository.findById(10L)).thenReturn(Optional.of(school));
        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> studentService.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void findPageReturnsOnlyTheRequestedSliceWithNormalizedSearch() {
        Student student = new Student();
        student.setId(40L);
        student.setSchool(school);
        student.setEnrollmentNumber("MAT-001");
        student.setFirstName("Ana");
        student.setLastName("Pérez");
        student.setGradeName("Primer grado");
        student.setGroupName("A");
        student.setSchoolGroup(group);
        student.setActive(true);

        when(schoolRepository.findById(10L))
                .thenReturn(Optional.of(school));
        when(studentRepository.searchBySchool(
                eq(10L),
                eq("ana pérez"),
                eq(20L),
                eq(30L),
                eq(true),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(student)));

        StudentPageResponse response = studentService.findPageBySchool(
                10L,
                0,
                25,
                "  Ana Pérez  ",
                20L,
                30L,
                true,
                "studentName",
                "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertEquals("MAT-001", response.content().getFirst().enrollmentNumber());
        verify(schoolAccessService).requireAccessToSchool(10L);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(studentRepository).searchBySchool(
                eq(10L),
                eq("ana pérez"),
                eq(20L),
                eq(30L),
                eq(true),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertEquals(
                org.springframework.data.domain.Sort.Direction.DESC,
                pageable.getSort().getOrderFor("lastName").getDirection()
        );
    }

    @Test
    void updateChangesStudentAndAssignedGroup() {
        Student student = new Student();
        student.setId(40L);
        student.setSchool(school);
        student.setEnrollmentNumber("MAT-001");
        student.setFirstName("Ana");
        student.setLastName("Pérez");
        student.setGradeName("Segundo grado");
        student.setGroupName("B");
        student.setActive(true);

        UpdateStudentRequest request = new UpdateStudentRequest(
                " mat-002 ",
                " Mariana ",
                " López ",
                30L
        );

        when(studentRepository.findById(40L))
                .thenReturn(Optional.of(student));
        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(studentRepository.save(student))
                .thenReturn(student);

        StudentResponse response = studentService.update(40L, request);

        assertEquals("MAT-002", response.enrollmentNumber());
        assertEquals("Mariana", response.firstName());
        assertEquals("López", response.lastName());
        assertEquals("Primer grado", response.gradeName());
        assertEquals("A", response.groupName());
        assertEquals(30L, response.schoolGroupId());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void updateRejectsEnrollmentUsedByAnotherStudent() {
        Student student = new Student();
        student.setId(40L);
        student.setSchool(school);

        UpdateStudentRequest request = new UpdateStudentRequest(
                "MAT-002",
                "Ana",
                "Pérez",
                30L
        );

        when(studentRepository.findById(40L))
                .thenReturn(Optional.of(student));
        when(schoolGroupRepository.findById(30L))
                .thenReturn(Optional.of(group));
        when(studentRepository
                .existsBySchool_IdAndEnrollmentNumberIgnoreCaseAndIdNot(
                        10L,
                        "MAT-002",
                        40L
                ))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> studentService.update(40L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(studentRepository, never()).save(any());
    }
}
