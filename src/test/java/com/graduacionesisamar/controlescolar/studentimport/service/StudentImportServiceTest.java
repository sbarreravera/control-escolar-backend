package com.graduacionesisamar.controlescolar.studentimport.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportResultResponse;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportTemplate;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportValidationResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentImportServiceTest {

    @Mock
    private AcademicCycleRepository academicCycleRepository;

    @Mock
    private SchoolGroupRepository schoolGroupRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    private StudentImportService service;
    private AcademicCycle cycle;
    private SchoolGroup firstGroup;
    private SchoolGroup thirdGroup;

    @BeforeEach
    void setUp() {
        service = new StudentImportService(
                academicCycleRepository,
                schoolGroupRepository,
                studentRepository,
                schoolAccessService
        );

        School school = new School();
        school.setId(10L);
        school.setName("Colegio San Felipe de Jesús");

        cycle = new AcademicCycle();
        cycle.setId(20L);
        cycle.setSchool(school);
        cycle.setName("2026-2027");
        cycle.setStartDate(LocalDate.of(2026, 8, 31));
        cycle.setEndDate(LocalDate.of(2027, 7, 9));
        cycle.setActive(true);

        firstGroup = group(30L, "1er Semestre", "Grupo 1");
        thirdGroup = group(31L, "3er Semestre", "Grupo 2");

        when(academicCycleRepository.findById(20L))
                .thenReturn(Optional.of(cycle));
        when(schoolGroupRepository
                .findAllByAcademicCycle_IdOrderByGradeNameAscGroupNameAsc(20L))
                .thenReturn(List.of(firstGroup, thirdGroup));
    }

    @Test
    void generatesOfficialTemplateWithSchoolCycleAndGroups()
            throws IOException {
        StudentImportTemplate template = service.generateTemplate(20L);

        assertEquals(
                "carga-inicial-alumnos-2026-2027.xlsx",
                template.fileName()
        );
        assertTrue(template.content().length > 0);

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(template.content())
        )) {
            assertNotNull(workbook.getSheet("Instrucciones"));
            Sheet students = workbook.getSheet("Alumnos");
            Sheet catalog = workbook.getSheet("Catálogos");

            assertNotNull(students);
            assertNotNull(catalog);
            assertEquals("Matrícula", students.getRow(0)
                    .getCell(0).getStringCellValue());
            assertEquals("Grado y grupo", students.getRow(0)
                    .getCell(3).getStringCellValue());
            assertEquals(
                    "1er Semestre | Grupo 1",
                    catalog.getRow(1).getCell(0).getStringCellValue()
            );
            assertFalse(students.getDataValidations().isEmpty());
        }

        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void validatesACompleteWorkbookAndPreservesNumericEnrollment()
            throws IOException {
        when(studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(10L))
                .thenReturn(List.of());

        MockMultipartFile file = workbookFile(List.<Object[]>of(
                new Object[]{
                        251130702303264d,
                        "Ana María",
                        "Pérez López",
                        "1er Semestre | Grupo 1"
                }
        ));

        StudentImportValidationResponse response = service.validate(
                20L,
                file
        );

        assertEquals(1, response.totalRows());
        assertEquals(1, response.validRows());
        assertEquals(0, response.invalidRows());
        assertTrue(response.canImport());
        assertTrue(response.errors().isEmpty());
    }

    @Test
    void reportsDuplicateExistingEnrollmentAndUnknownGroup()
            throws IOException {
        Student existing = new Student();
        existing.setEnrollmentNumber("MAT-EXISTENTE");

        when(studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(10L))
                .thenReturn(List.of(existing));

        MockMultipartFile file = workbookFile(List.of(
                new Object[]{
                        "MAT-EXISTENTE",
                        "Ana",
                        "Pérez",
                        "Grupo inexistente"
                },
                new Object[]{
                        "MAT-DUP",
                        "Luis",
                        "López",
                        "1er Semestre | Grupo 1"
                },
                new Object[]{
                        "mat-dup",
                        "María",
                        "Sánchez",
                        "3er Semestre | Grupo 2"
                }
        ));

        StudentImportValidationResponse response = service.validate(
                20L,
                file
        );

        assertEquals(3, response.totalRows());
        assertEquals(0, response.validRows());
        assertEquals(3, response.invalidRows());
        assertFalse(response.canImport());
        assertTrue(response.errors().get(0).messages().stream()
                .anyMatch(message -> message.contains("ya está registrada")));
        assertTrue(response.errors().get(0).messages().stream()
                .anyMatch(message -> message.contains("no pertenece")));
        assertTrue(response.errors().get(1).messages().stream()
                .anyMatch(message -> message.contains("repetida")));
        assertTrue(response.errors().get(2).messages().stream()
                .anyMatch(message -> message.contains("repetida")));
    }

    @Test
    void importsEveryValidRowInOneBatch() throws IOException {
        when(studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(10L))
                .thenReturn(List.of());

        MockMultipartFile file = workbookFile(List.of(
                new Object[]{
                        "mat-001",
                        " Ana ",
                        " Pérez ",
                        "1er Semestre | Grupo 1"
                },
                new Object[]{
                        "mat-002",
                        "Luis",
                        "López",
                        "3er Semestre | Grupo 2"
                }
        ));

        StudentImportResultResponse response = service.importStudents(
                20L,
                file
        );

        assertEquals(2, response.importedRows());
        assertTrue(response.validation().canImport());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Student>> captor = ArgumentCaptor
                .forClass(List.class);
        verify(studentRepository).saveAll(captor.capture());

        List<Student> students = captor.getValue();
        assertEquals("MAT-001", students.get(0).getEnrollmentNumber());
        assertEquals("Ana", students.get(0).getFirstName());
        assertEquals(30L, students.get(0).getSchoolGroup().getId());
        assertEquals("MAT-002", students.get(1).getEnrollmentNumber());
        assertEquals(31L, students.get(1).getSchoolGroup().getId());
    }

    @Test
    void doesNotSaveAnyStudentWhenOneRowIsInvalid()
            throws IOException {
        when(studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(10L))
                .thenReturn(List.of());

        MockMultipartFile file = workbookFile(List.of(
                new Object[]{
                        "MAT-001",
                        "Ana",
                        "Pérez",
                        "1er Semestre | Grupo 1"
                },
                new Object[]{
                        "",
                        "Luis",
                        "López",
                        "3er Semestre | Grupo 2"
                }
        ));

        StudentImportResultResponse response = service.importStudents(
                20L,
                file
        );

        assertEquals(0, response.importedRows());
        assertFalse(response.validation().canImport());
        verify(studentRepository, never()).saveAll(anyList());
    }

    private SchoolGroup group(
            Long id,
            String gradeName,
            String groupName
    ) {
        SchoolGroup group = new SchoolGroup();
        group.setId(id);
        group.setAcademicCycle(cycle);
        group.setGradeName(gradeName);
        group.setGroupName(groupName);
        group.setActive(true);
        return group;
    }

    private MockMultipartFile workbookFile(List<Object[]> rows)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Alumnos");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Matrícula");
            header.createCell(1).setCellValue("Nombre(s)");
            header.createCell(2).setCellValue("Apellidos");
            header.createCell(3).setCellValue("Grado y grupo");

            for (int index = 0; index < rows.size(); index++) {
                Row row = sheet.createRow(index + 1);
                Object[] values = rows.get(index);

                for (int column = 0; column < values.length; column++) {
                    Object value = values[column];
                    if (value instanceof Number number) {
                        row.createCell(column).setCellValue(
                                number.doubleValue()
                        );
                    } else {
                        row.createCell(column).setCellValue(
                                String.valueOf(value)
                        );
                    }
                }
            }

            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "alumnos.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }
}
