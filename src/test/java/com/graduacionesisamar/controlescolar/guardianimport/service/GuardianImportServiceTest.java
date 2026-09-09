package com.graduacionesisamar.controlescolar.guardianimport.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportResultResponse;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportTemplate;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportValidationResponse;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
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
class GuardianImportServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    @Mock
    private GuardianAccountService guardianAccountService;

    private GuardianImportService service;
    private School school;
    private Student firstStudent;
    private Student secondStudent;
    private Guardian existingGuardian;

    @BeforeEach
    void setUp() {
        service = new GuardianImportService(
                schoolRepository,
                studentRepository,
                guardianRepository,
                studentGuardianRepository,
                schoolAccessService,
                guardianAccountService
        );

        school = new School();
        school.setId(10L);
        school.setName("Escuela de Prueba 2");

        firstStudent = student(20L, "MAT-001", "Alumno2", "Pérez");
        secondStudent = student(21L, "MAT-002", "Luis", "Ejemplo2");

        existingGuardian = new Guardian();
        existingGuardian.setId(30L);
        existingGuardian.setSchool(school);
        existingGuardian.setExternalReference("TUT-EXISTE");
        existingGuardian.setFullName("Tutor de Prueba 4");
        existingGuardian.setPhone("5550000403");
        existingGuardian.setEmail("guardian.2@example.test");
        existingGuardian.setActive(true);

        when(schoolRepository.findById(10L))
                .thenReturn(Optional.of(school));
        when(studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(10L))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(guardianRepository
                .findAllBySchool_IdOrderByFullNameAsc(10L))
                .thenReturn(List.of(existingGuardian));
        when(studentGuardianRepository.findAllByStudent_School_Id(10L))
                .thenReturn(List.of());
    }

    @Test
    void generatesOfficialTemplateWithStudentsAndValidationLists()
            throws IOException {
        GuardianImportTemplate template = service.generateTemplate(10L);

        assertEquals(
                "carga-tutores-relaciones-colegio-san-felipe-de-jesus.xlsx",
                template.fileName()
        );

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(template.content())
        )) {
            assertNotNull(workbook.getSheet("Instrucciones"));
            Sheet data = workbook.getSheet("Tutores y alumnos");
            Sheet catalog = workbook.getSheet("Catálogos");

            assertNotNull(data);
            assertNotNull(catalog);
            assertEquals(
                    "Clave del tutor",
                    data.getRow(0).getCell(0).getStringCellValue()
            );
            assertEquals(
                    "MAT-001",
                    catalog.getRow(1).getCell(0).getStringCellValue()
            );
            assertEquals(
                    "TUT-EXISTE",
                    catalog.getRow(1).getCell(4).getStringCellValue()
            );
            assertEquals(3, data.getDataValidations().size());
        }

        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void validatesNewAndExistingGuardiansWithoutDuplicatingSiblings()
            throws IOException {
        MockMultipartFile file = workbookFile(List.of(
                row(
                        "tut-nuevo",
                        "José Ejemplo2",
                        "5550000404",
                        "guardian.5@example.test",
                        "MAT-001",
                        "Padre",
                        "Sí",
                        "Sí"
                ),
                row(
                        "TUT-NUEVO",
                        "José Ejemplo2",
                        "5550000404",
                        "guardian.6@example.test",
                        "MAT-002",
                        "Padre",
                        "Sí",
                        "Sí"
                ),
                row(
                        "TUT-EXISTE",
                        "Tutor de Prueba 4",
                        "5550000403",
                        "guardian.2@example.test",
                        "MAT-002",
                        "Madre",
                        "No",
                        "Sí"
                )
        ));

        GuardianImportValidationResponse response = service.validate(
                10L,
                file
        );

        assertEquals(3, response.totalRows());
        assertEquals(3, response.validRows());
        assertEquals(1, response.guardiansToCreate());
        assertEquals(1, response.guardiansToReuse());
        assertEquals(3, response.relationshipsToCreate());
        assertTrue(response.canImport());
    }

    @Test
    void reportsUnknownStudentsInvalidBooleansAndDuplicateRelationships()
            throws IOException {
        MockMultipartFile file = workbookFile(List.of(
                row(
                        "TUT-001",
                        "José Ejemplo2",
                        "",
                        "correo-invalido",
                        "NO-EXISTE",
                        "Padre",
                        "Tal vez",
                        "Sí"
                ),
                row(
                        "TUT-002",
                        "Laura Pérez",
                        "",
                        "",
                        "MAT-001",
                        "Madre",
                        "No",
                        "No"
                ),
                row(
                        "tut-002",
                        "Laura Pérez",
                        "",
                        "",
                        "mat-001",
                        "Madre",
                        "No",
                        "No"
                )
        ));

        GuardianImportValidationResponse response = service.validate(
                10L,
                file
        );

        assertFalse(response.canImport());
        assertEquals(3, response.invalidRows());
        assertTrue(response.errors().get(0).messages().stream()
                .anyMatch(message -> message.contains("formato válido")));
        assertTrue(response.errors().get(0).messages().stream()
                .anyMatch(message -> message.contains("alumno activo")));
        assertTrue(response.errors().get(1).messages().stream()
                .anyMatch(message -> message.contains("repetida")));
        assertTrue(response.errors().get(2).messages().stream()
                .anyMatch(message -> message.contains("repetida")));
    }

    @Test
    void rejectsDifferentPersonalDataForTheSameGuardianKey()
            throws IOException {
        MockMultipartFile file = workbookFile(List.of(
                row(
                        "TUT-001",
                        "José Ejemplo2",
                        "5550000404",
                        "guardian.5@example.test",
                        "MAT-001",
                        "Padre",
                        "No",
                        "Sí"
                ),
                row(
                        "TUT-001",
                        "José Ejemplo2",
                        "5550000405",
                        "guardian.5@example.test",
                        "MAT-002",
                        "Padre",
                        "No",
                        "Sí"
                )
        ));

        GuardianImportValidationResponse response = service.validate(
                10L,
                file
        );

        assertFalse(response.canImport());
        assertEquals(2, response.invalidRows());
        assertTrue(response.errors().stream().allMatch(error ->
                error.messages().stream().anyMatch(message ->
                        message.contains("datos personales diferentes")
                )
        ));
    }

    @Test
    void importsNewAndExistingGuardiansInOneTransaction()
            throws IOException {
        when(guardianRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<Guardian> guardians = invocation.getArgument(0);
                    for (int index = 0; index < guardians.size(); index++) {
                        guardians.get(index).setId(100L + index);
                    }
                    return guardians;
                });

        MockMultipartFile file = workbookFile(List.of(
                row(
                        "TUT-NUEVO",
                        "José Ejemplo2",
                        "5550000404",
                        "guardian.5@example.test",
                        "MAT-001",
                        "Padre",
                        "Sí",
                        "Sí"
                ),
                row(
                        "TUT-EXISTE",
                        "Tutor de Prueba 4",
                        "5550000403",
                        "guardian.2@example.test",
                        "MAT-002",
                        "Madre",
                        "Sí",
                        "Sí"
                )
        ));

        GuardianImportResultResponse response = service.importGuardians(
                10L,
                file
        );

        assertEquals(1, response.guardiansCreated());
        assertEquals(1, response.guardiansReused());
        assertEquals(2, response.relationshipsCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentGuardian>> relationshipCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(studentGuardianRepository).saveAll(
                relationshipCaptor.capture()
        );
        assertEquals(2, relationshipCaptor.getValue().size());
        assertEquals(
                100L,
                relationshipCaptor.getValue().get(0).getGuardian().getId()
        );
        assertEquals(
                30L,
                relationshipCaptor.getValue().get(1).getGuardian().getId()
        );
        verify(guardianAccountService).ensureAccounts(anyList());
    }

    @Test
    void doesNotSaveAnythingWhenOneRowIsInvalid() throws IOException {
        MockMultipartFile file = workbookFile(List.of(
                row(
                        "TUT-001",
                        "José Ejemplo2",
                        "",
                        "",
                        "MAT-001",
                        "Padre",
                        "Sí",
                        "Sí"
                ),
                row(
                        "TUT-002",
                        "Laura Pérez",
                        "",
                        "",
                        "NO-EXISTE",
                        "Madre",
                        "No",
                        "Sí"
                )
        ));

        GuardianImportResultResponse response = service.importGuardians(
                10L,
                file
        );

        assertFalse(response.validation().canImport());
        assertEquals(0, response.guardiansCreated());
        assertEquals(0, response.relationshipsCreated());
        verify(guardianRepository, never()).saveAll(anyList());
        verify(studentGuardianRepository, never()).saveAll(anyList());
    }

    private Student student(
            Long id,
            String enrollment,
            String firstName,
            String lastName
    ) {
        Student student = new Student();
        student.setId(id);
        student.setSchool(school);
        student.setEnrollmentNumber(enrollment);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setActive(true);
        return student;
    }

    private Object[] row(Object... values) {
        return values;
    }

    private MockMultipartFile workbookFile(List<Object[]> rows)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Tutores y alumnos");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Clave del tutor");
            header.createCell(1).setCellValue("Nombre completo");
            header.createCell(2).setCellValue("Teléfono");
            header.createCell(3).setCellValue("Correo");
            header.createCell(4).setCellValue("Matrícula del alumno");
            header.createCell(5).setCellValue("Parentesco");
            header.createCell(6).setCellValue("Contacto principal");
            header.createCell(7).setCellValue("Recibe notificaciones");

            for (int index = 0; index < rows.size(); index++) {
                Row row = sheet.createRow(index + 1);
                Object[] values = rows.get(index);
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(
                            String.valueOf(values[column])
                    );
                }
            }

            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "tutores.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }
}
