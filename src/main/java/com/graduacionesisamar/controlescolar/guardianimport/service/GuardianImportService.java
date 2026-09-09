package com.graduacionesisamar.controlescolar.guardianimport.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportResultResponse;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportRowErrorResponse;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportTemplate;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportValidationResponse;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardianId;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates, validates and imports guardians and student relationships.
 */
@Service
@RequiredArgsConstructor
public class GuardianImportService {

    static final String DATA_SHEET = "Tutores y alumnos";
    static final String CATALOG_SHEET = "Catálogos";
    static final String INSTRUCTIONS_SHEET = "Instrucciones";

    private static final String[] HEADERS = {
            "Clave del tutor",
            "Nombre completo",
            "Teléfono",
            "Correo",
            "Matrícula del alumno",
            "Parentesco",
            "Contacto principal",
            "Recibe notificaciones"
    };

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_DATA_ROWS = 2000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SchoolAccessService schoolAccessService;
    private final GuardianAccountService guardianAccountService;

    /**
     * Generates a workbook tied to the authenticated user's school.
     */
    @Transactional(readOnly = true)
    public GuardianImportTemplate generateTemplate(Long schoolId) {
        ImportContext context = loadContext(schoolId);

        if (context.students().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The school has no active students"
            );
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            createInstructionsSheet(workbook, context.school());
            createCatalogSheet(
                    workbook,
                    context.students(),
                    context.guardians()
            );
            createDataSheet(workbook, context.students().size());
            workbook.setSheetOrder(DATA_SHEET, 1);
            workbook.setActiveSheet(1);
            workbook.write(output);

            return new GuardianImportTemplate(
                    "carga-tutores-relaciones-"
                            + safeFilePart(context.school().getName())
                            + ".xlsx",
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not generate the guardian import template",
                    exception
            );
        }
    }

    /**
     * Validates the workbook without changing data.
     */
    @Transactional(readOnly = true)
    public GuardianImportValidationResponse validate(
            Long schoolId,
            MultipartFile file
    ) {
        return analyze(loadContext(schoolId), file).validation();
    }

    /**
     * Imports guardians and relationships atomically after revalidation.
     */
    @Transactional
    public GuardianImportResultResponse importGuardians(
            Long schoolId,
            MultipartFile file
    ) {
        ImportContext context = loadContext(schoolId);
        Analysis analysis = analyze(context, file);
        GuardianImportValidationResponse validation = analysis.validation();

        if (!validation.canImport()) {
            return new GuardianImportResultResponse(0, 0, 0, validation);
        }

        Map<String, ParsedRow> firstRowByNewGuardian = analysis.rows()
                .stream()
                .filter(row -> row.existingGuardian() == null)
                .collect(Collectors.toMap(
                        ParsedRow::externalReference,
                        Function.identity(),
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ));

        List<Guardian> newGuardians = firstRowByNewGuardian.values()
                .stream()
                .map(row -> buildGuardian(context.school(), row))
                .toList();

        guardianRepository.saveAll(newGuardians);
        guardianRepository.flush();

        Map<String, Guardian> guardiansByReference = context.guardians()
                .stream()
                .filter(guardian -> guardian.getExternalReference() != null)
                .collect(Collectors.toMap(
                        guardian -> normalizeExternalReference(
                                guardian.getExternalReference()
                        ),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        newGuardians.forEach(guardian -> guardiansByReference.put(
                guardian.getExternalReference(),
                guardian
        ));

        guardianAccountService.ensureAccounts(
                List.copyOf(guardiansByReference.values())
        );

        List<StudentGuardian> relationships = analysis.rows().stream()
                .map(row -> buildRelationship(
                        row,
                        guardiansByReference.get(row.externalReference())
                ))
                .toList();

        studentGuardianRepository.saveAll(relationships);

        return new GuardianImportResultResponse(
                newGuardians.size(),
                validation.guardiansToReuse(),
                relationships.size(),
                validation
        );
    }

    private ImportContext loadContext(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School not found"
                ));

        List<Student> students = studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(schoolId)
                .stream()
                .filter(student -> Boolean.TRUE.equals(student.getActive()))
                .toList();
        List<Guardian> guardians = guardianRepository
                .findAllBySchool_IdOrderByFullNameAsc(schoolId);
        List<StudentGuardian> relationships = studentGuardianRepository
                .findAllByStudent_School_Id(schoolId);

        return new ImportContext(
                school,
                students,
                guardians,
                relationships
        );
    }

    private Analysis analyze(
            ImportContext context,
            MultipartFile file
    ) {
        List<GuardianImportRowErrorResponse> fileErrors = validateFile(file);
        if (!fileErrors.isEmpty()) {
            return emptyAnalysis(fileErrors);
        }

        Map<String, Student> studentsByEnrollment = context.students()
                .stream()
                .collect(Collectors.toMap(
                        student -> normalizeEnrollment(
                                student.getEnrollmentNumber()
                        ),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        Map<String, Guardian> guardiansByReference = context.guardians()
                .stream()
                .filter(guardian -> guardian.getExternalReference() != null)
                .collect(Collectors.toMap(
                        guardian -> normalizeExternalReference(
                                guardian.getExternalReference()
                        ),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        Set<RelationshipKey> existingRelationships = context.relationships()
                .stream()
                .map(relationship -> new RelationshipKey(
                        relationship.getStudent().getId(),
                        relationship.getGuardian().getId()
                ))
                .collect(Collectors.toSet());
        Set<Long> studentsWithPrimaryContact = context.relationships()
                .stream()
                .filter(relationship -> Boolean.TRUE.equals(
                        relationship.getPrimaryContact()
                ))
                .map(relationship -> relationship.getStudent().getId())
                .collect(Collectors.toSet());

        try (InputStream input = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet(DATA_SHEET);
            if (sheet == null) {
                return emptyAnalysis(List.of(fileError(
                        "No se encontró la hoja obligatoria \"Tutores y alumnos\"."
                )));
            }

            List<GuardianImportRowErrorResponse> headerErrors =
                    validateHeaders(sheet);
            if (!headerErrors.isEmpty()) {
                return emptyAnalysis(headerErrors);
            }

            if (sheet.getLastRowNum() > MAX_DATA_ROWS) {
                return emptyAnalysis(List.of(fileError(
                        "El archivo supera el máximo de "
                                + MAX_DATA_ROWS
                                + " relaciones."
                )));
            }

            FormulaEvaluator evaluator = workbook
                    .getCreationHelper()
                    .createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(
                    Locale.forLanguageTag("es-MX")
            );
            List<ParsedRow> rows = new ArrayList<>();

            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row excelRow = sheet.getRow(index);
                if (isBlankRow(excelRow, formatter, evaluator)) {
                    continue;
                }

                rows.add(parseRow(
                        excelRow,
                        index + 1,
                        studentsByEnrollment,
                        guardiansByReference,
                        existingRelationships,
                        formatter,
                        evaluator
                ));
            }

            addGuardianConsistencyErrors(rows);
            addDuplicateRelationshipErrors(rows);
            addPrimaryContactErrors(rows, studentsWithPrimaryContact);
            return buildAnalysis(rows);
        } catch (IOException | RuntimeException exception) {
            return emptyAnalysis(List.of(fileError(
                    "No fue posible leer el archivo. Descarga nuevamente el formato oficial."
            )));
        }
    }

    private ParsedRow parseRow(
            Row row,
            int rowNumber,
            Map<String, Student> studentsByEnrollment,
            Map<String, Guardian> guardiansByReference,
            Set<RelationshipKey> existingRelationships,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        String reference = normalizeExternalReference(identifierCellValue(
                row, 0, formatter, evaluator
        ));
        String fullName = normalizeText(cellValue(
                row, 1, formatter, evaluator
        ));
        String phone = normalizeNullable(identifierCellValue(
                row, 2, formatter, evaluator
        ));
        String email = normalizeEmail(cellValue(
                row, 3, formatter, evaluator
        ));
        String enrollment = normalizeEnrollment(identifierCellValue(
                row, 4, formatter, evaluator
        ));
        String relationship = normalizeNullable(cellValue(
                row, 5, formatter, evaluator
        ));
        Boolean primaryContact = parseYesNo(cellValue(
                row, 6, formatter, evaluator
        ));
        Boolean receivesNotifications = parseYesNo(cellValue(
                row, 7, formatter, evaluator
        ));

        List<String> errors = new ArrayList<>();
        validateRequiredAndLength(
                reference,
                "La clave del tutor es obligatoria.",
                "La clave del tutor no puede superar 50 caracteres.",
                50,
                errors
        );
        validateRequiredAndLength(
                fullName,
                "El nombre completo es obligatorio.",
                "El nombre completo no puede superar 150 caracteres.",
                150,
                errors
        );
        validateOptionalLength(
                phone,
                "El teléfono no puede superar 30 caracteres.",
                30,
                errors
        );
        validateOptionalLength(
                email,
                "El correo no puede superar 150 caracteres.",
                150,
                errors
        );
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("El correo electrónico no tiene un formato válido.");
        }
        validateRequiredAndLength(
                enrollment,
                "La matrícula del alumno es obligatoria.",
                "La matrícula no puede superar 50 caracteres.",
                50,
                errors
        );
        validateOptionalLength(
                relationship,
                "El parentesco no puede superar 50 caracteres.",
                50,
                errors
        );
        if (primaryContact == null) {
            errors.add("Contacto principal debe ser Sí o No.");
        }
        if (receivesNotifications == null) {
            errors.add("Recibe notificaciones debe ser Sí o No.");
        }

        Student student = studentsByEnrollment.get(enrollment);
        if (!enrollment.isBlank() && student == null) {
            errors.add(
                    "La matrícula no pertenece a un alumno activo de esta escuela."
            );
        }

        Guardian existingGuardian = guardiansByReference.get(reference);
        if (existingGuardian != null) {
            validateExistingGuardian(existingGuardian, fullName, phone, email, errors);

            if (student != null && existingRelationships.contains(
                    new RelationshipKey(student.getId(), existingGuardian.getId())
            )) {
                errors.add(
                        "El tutor ya está relacionado con este alumno."
                );
            }
        }

        return new ParsedRow(
                rowNumber,
                reference,
                fullName,
                phone,
                email,
                enrollment,
                relationship,
                primaryContact,
                receivesNotifications,
                student,
                existingGuardian,
                errors
        );
    }

    private void validateExistingGuardian(
            Guardian guardian,
            String fullName,
            String phone,
            String email,
            List<String> errors
    ) {
        if (!Boolean.TRUE.equals(guardian.getActive())) {
            errors.add("La clave corresponde a un tutor inactivo.");
        }
        if (!canonical(guardian.getFullName()).equals(canonical(fullName))) {
            errors.add(
                    "El nombre no coincide con el tutor que ya usa esta clave."
            );
        }
        if (!Objects.equals(normalizeNullable(guardian.getPhone()), phone)) {
            errors.add(
                    "El teléfono no coincide con el tutor que ya usa esta clave."
            );
        }
        if (!Objects.equals(normalizeEmail(guardian.getEmail()), email)) {
            errors.add(
                    "El correo no coincide con el tutor que ya usa esta clave."
            );
        }
    }

    private void addGuardianConsistencyErrors(List<ParsedRow> rows) {
        Map<String, List<ParsedRow>> rowsByReference = rows.stream()
                .filter(row -> !row.externalReference().isBlank())
                .collect(Collectors.groupingBy(
                        ParsedRow::externalReference,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        rowsByReference.values().forEach(sameGuardianRows -> {
            ParsedRow first = sameGuardianRows.getFirst();
            boolean inconsistent = sameGuardianRows.stream().anyMatch(row ->
                    !canonical(row.fullName()).equals(canonical(first.fullName()))
                            || !Objects.equals(row.phone(), first.phone())
                            || !Objects.equals(row.email(), first.email())
            );

            if (inconsistent) {
                sameGuardianRows.forEach(row -> row.errors().add(
                        "La misma clave de tutor tiene datos personales diferentes dentro del archivo."
                ));
            }
        });
    }

    private void addDuplicateRelationshipErrors(List<ParsedRow> rows) {
        Map<FileRelationshipKey, Integer> counts = new HashMap<>();
        rows.stream()
                .filter(row -> !row.externalReference().isBlank())
                .filter(row -> !row.enrollmentNumber().isBlank())
                .map(row -> new FileRelationshipKey(
                        row.externalReference(),
                        row.enrollmentNumber()
                ))
                .forEach(key -> counts.merge(key, 1, Integer::sum));

        rows.stream()
                .filter(row -> counts.getOrDefault(
                        new FileRelationshipKey(
                                row.externalReference(),
                                row.enrollmentNumber()
                        ),
                        0
                ) > 1)
                .forEach(row -> row.errors().add(
                        "La relación tutor-alumno está repetida dentro del archivo."
                ));
    }

    private void addPrimaryContactErrors(
            List<ParsedRow> rows,
            Set<Long> studentsWithPrimaryContact
    ) {
        Map<Long, Integer> primaryRowsByStudent = new HashMap<>();
        rows.stream()
                .filter(row -> Boolean.TRUE.equals(row.primaryContact()))
                .filter(row -> row.student() != null)
                .forEach(row -> primaryRowsByStudent.merge(
                        row.student().getId(),
                        1,
                        Integer::sum
                ));

        rows.stream()
                .filter(row -> Boolean.TRUE.equals(row.primaryContact()))
                .filter(row -> row.student() != null)
                .forEach(row -> {
                    Long studentId = row.student().getId();
                    if (studentsWithPrimaryContact.contains(studentId)) {
                        row.errors().add(
                                "El alumno ya tiene un contacto principal registrado."
                        );
                    }
                    if (primaryRowsByStudent.getOrDefault(studentId, 0) > 1) {
                        row.errors().add(
                                "El alumno tiene más de un contacto principal dentro del archivo."
                        );
                    }
                });
    }

    private Analysis buildAnalysis(List<ParsedRow> rows) {
        List<GuardianImportRowErrorResponse> errors = rows.stream()
                .filter(row -> !row.errors().isEmpty())
                .map(row -> new GuardianImportRowErrorResponse(
                        row.rowNumber(),
                        blankToNull(row.externalReference()),
                        blankToNull(row.enrollmentNumber()),
                        List.copyOf(row.errors())
                ))
                .toList();

        int totalRows = rows.size();
        int invalidRows = errors.size();
        int validRows = totalRows - invalidRows;

        if (totalRows == 0) {
            errors = List.of(fileError(
                    "El archivo no contiene tutores y relaciones para importar."
            ));
        }

        List<ParsedRow> validParsedRows = rows.stream()
                .filter(row -> row.errors().isEmpty())
                .toList();
        int guardiansToCreate = (int) validParsedRows.stream()
                .filter(row -> row.existingGuardian() == null)
                .map(ParsedRow::externalReference)
                .distinct()
                .count();
        int guardiansToReuse = (int) validParsedRows.stream()
                .map(ParsedRow::existingGuardian)
                .filter(Objects::nonNull)
                .map(Guardian::getId)
                .distinct()
                .count();

        GuardianImportValidationResponse validation =
                new GuardianImportValidationResponse(
                        totalRows,
                        validRows,
                        invalidRows,
                        guardiansToCreate,
                        guardiansToReuse,
                        validRows,
                        totalRows > 0 && invalidRows == 0,
                        errors
                );

        return new Analysis(rows, validation);
    }

    private Analysis emptyAnalysis(
            List<GuardianImportRowErrorResponse> errors
    ) {
        return new Analysis(
                List.of(),
                new GuardianImportValidationResponse(
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        false,
                        errors
                )
        );
    }

    private Guardian buildGuardian(School school, ParsedRow row) {
        Guardian guardian = new Guardian();
        guardian.setSchool(school);
        guardian.setExternalReference(row.externalReference());
        guardian.setFullName(row.fullName());
        guardian.setPhone(row.phone());
        guardian.setEmail(row.email());
        return guardian;
    }

    private StudentGuardian buildRelationship(
            ParsedRow row,
            Guardian guardian
    ) {
        StudentGuardian relationship = new StudentGuardian();
        relationship.setId(new StudentGuardianId(
                row.student().getId(),
                guardian.getId()
        ));
        relationship.setStudent(row.student());
        relationship.setGuardian(guardian);
        relationship.setRelationship(row.relationship());
        relationship.setPrimaryContact(row.primaryContact());
        relationship.setReceivesNotifications(
                row.receivesNotifications()
        );
        return relationship;
    }

    private List<GuardianImportRowErrorResponse> validateFile(
            MultipartFile file
    ) {
        List<GuardianImportRowErrorResponse> errors = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            errors.add(fileError("Selecciona un archivo de Excel."));
            return errors;
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            errors.add(fileError("El archivo debe tener extensión .xlsx."));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            errors.add(fileError("El archivo no puede superar 5 MB."));
        }
        return errors;
    }

    private List<GuardianImportRowErrorResponse> validateHeaders(
            Sheet sheet
    ) {
        Row header = sheet.getRow(0);
        if (header == null) {
            return List.of(fileError(
                    "La hoja \"Tutores y alumnos\" no contiene encabezados."
            ));
        }

        DataFormatter formatter = new DataFormatter();
        List<String> messages = new ArrayList<>();
        for (int column = 0; column < HEADERS.length; column++) {
            String actual = formatter.formatCellValue(header.getCell(
                    column,
                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
            ));
            if (!canonical(actual).equals(canonical(HEADERS[column]))) {
                messages.add(
                        "La columna "
                                + (column + 1)
                                + " debe llamarse \""
                                + HEADERS[column]
                                + "\"."
                );
            }
        }

        return messages.isEmpty()
                ? List.of()
                : List.of(new GuardianImportRowErrorResponse(
                        1,
                        null,
                        null,
                        messages
                ));
    }

    private void createInstructionsSheet(
            Workbook workbook,
            School school
    ) {
        Sheet sheet = workbook.createSheet(INSTRUCTIONS_SHEET);
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle labelStyle = labelStyle(workbook);
        CellStyle wrapStyle = wrapStyle(workbook);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(28);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Carga de tutores y relaciones");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        createLabelValueRow(
                sheet,
                2,
                "Escuela",
                school.getName(),
                labelStyle
        );

        Row instructionsTitle = sheet.createRow(4);
        instructionsTitle.createCell(0).setCellValue("Instrucciones");
        instructionsTitle.getCell(0).setCellStyle(labelStyle);

        String[] instructions = {
                "1. Captura los datos únicamente en la hoja \"Tutores y alumnos\".",
                "2. No cambies los nombres ni el orden de las columnas.",
                "3. Asigna una clave estable a cada tutor, por ejemplo TUT-001.",
                "4. Repite la misma clave y los mismos datos cuando un tutor tenga varios alumnos.",
                "5. No uses el nombre, teléfono o correo como clave para deduplicar tutores.",
                "6. Selecciona la matrícula y los valores Sí/No desde las listas del archivo.",
                "7. Solo puede existir un contacto principal por alumno.",
                "8. El sistema validará todas las filas y no guardará datos parciales."
        };

        for (int index = 0; index < instructions.length; index++) {
            Row row = sheet.createRow(5 + index);
            Cell cell = row.createCell(0);
            cell.setCellValue(instructions[index]);
            cell.setCellStyle(wrapStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                    5 + index,
                    5 + index,
                    0,
                    3
            ));
        }

        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 34 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        sheet.setColumnWidth(3, 24 * 256);
    }

    private void createDataSheet(Workbook workbook, int studentCount) {
        Sheet sheet = workbook.createSheet(DATA_SHEET);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle textStyle = textStyle(workbook);

        Row header = sheet.createRow(0);
        header.setHeightInPoints(30);
        for (int index = 0; index < HEADERS.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(headerStyle);
            sheet.setDefaultColumnStyle(index, textStyle);
        }

        int[] widths = {20, 34, 20, 34, 24, 20, 22, 24};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

        Name studentList = workbook.createName();
        studentList.setNameName("MATRICULAS_VALIDAS");
        studentList.setRefersToFormula(
                "'" + CATALOG_SHEET + "'!$A$2:$A$" + (studentCount + 1)
        );
        Name yesNoList = workbook.createName();
        yesNoList.setNameName("VALORES_SI_NO");
        yesNoList.setRefersToFormula(
                "'" + CATALOG_SHEET + "'!$D$2:$D$3"
        );

        addListValidation(
                sheet,
                "MATRICULAS_VALIDAS",
                4,
                "Matrícula no válida",
                "Selecciona una matrícula de la lista."
        );
        addListValidation(
                sheet,
                "VALORES_SI_NO",
                6,
                "Valor no válido",
                "Selecciona Sí o No."
        );
        addListValidation(
                sheet,
                "VALORES_SI_NO",
                7,
                "Valor no válido",
                "Selecciona Sí o No."
        );
    }

    private void createCatalogSheet(
            Workbook workbook,
            List<Student> students,
            List<Guardian> guardians
    ) {
        Sheet sheet = workbook.createSheet(CATALOG_SHEET);
        CellStyle headerStyle = headerStyle(workbook);

        String[] headers = {
                "Matrículas válidas",
                "Alumno",
                "Grado y grupo",
                "Valores Sí/No",
                "Claves de tutores existentes",
                "Tutor existente",
                "Teléfono existente",
                "Correo existente"
        };
        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            header.createCell(index).setCellValue(headers[index]);
            header.getCell(index).setCellStyle(headerStyle);
        }

        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            Row row = sheet.createRow(index + 1);
            row.createCell(0).setCellValue(student.getEnrollmentNumber());
            row.createCell(1).setCellValue(
                    student.getFirstName() + " " + student.getLastName()
            );
            row.createCell(2).setCellValue(studentGroupLabel(student));
        }
        getOrCreateRow(sheet, 1).createCell(3).setCellValue("Sí");
        getOrCreateRow(sheet, 2).createCell(3).setCellValue("No");

        List<Guardian> identifiedGuardians = guardians.stream()
                .filter(guardian -> guardian.getExternalReference() != null)
                .filter(guardian -> Boolean.TRUE.equals(guardian.getActive()))
                .toList();
        for (int index = 0; index < identifiedGuardians.size(); index++) {
            Guardian guardian = identifiedGuardians.get(index);
            Row row = getOrCreateRow(sheet, index + 1);
            row.createCell(4).setCellValue(guardian.getExternalReference());
            row.createCell(5).setCellValue(guardian.getFullName());
            row.createCell(6).setCellValue(
                    guardian.getPhone() == null ? "" : guardian.getPhone()
            );
            row.createCell(7).setCellValue(
                    guardian.getEmail() == null ? "" : guardian.getEmail()
            );
        }

        int[] widths = {24, 36, 32, 18, 30, 36, 24, 36};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private void addListValidation(
            Sheet sheet,
            String listName,
            int column,
            String errorTitle,
            String errorMessage
    ) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper
                .createFormulaListConstraint(listName);
        CellRangeAddressList range = new CellRangeAddressList(
                1,
                MAX_DATA_ROWS,
                column,
                column
        );
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setEmptyCellAllowed(false);
        validation.setShowErrorBox(true);
        validation.createErrorBox(errorTitle, errorMessage);
        sheet.addValidationData(validation);
    }

    private Row getOrCreateRow(Sheet sheet, int rowNumber) {
        Row row = sheet.getRow(rowNumber);
        return row == null ? sheet.createRow(rowNumber) : row;
    }

    private String studentGroupLabel(Student student) {
        if (student.getSchoolGroup() != null) {
            return student.getSchoolGroup().getGradeName()
                    + " | "
                    + student.getSchoolGroup().getGroupName();
        }
        String grade = normalizeText(student.getGradeName());
        String group = normalizeText(student.getGroupName());
        return (grade + " | " + group).replaceAll("(^ \\| | \\| $)", "");
    }

    private void createLabelValueRow(
            Sheet sheet,
            int rowNumber,
            String label,
            String value,
            CellStyle labelStyle
    ) {
        Row row = sheet.createRow(rowNumber);
        row.createCell(0).setCellValue(label);
        row.getCell(0).setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value);
        sheet.addMergedRegion(new CellRangeAddress(rowNumber, rowNumber, 1, 3));
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle labelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle wrapStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private CellStyle textStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("@"));
        return style;
    }

    private boolean isBlankRow(
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        if (row == null) {
            return true;
        }
        for (int column = 0; column < HEADERS.length; column++) {
            if (!cellValue(row, column, formatter, evaluator).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellValue(
            Row row,
            int column,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        Cell cell = row.getCell(
                column,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
        );
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    private String identifierCellValue(
            Row row,
            int column,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        Cell cell = row.getCell(
                column,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
        );
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return NumberToTextConverter.toText(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            CellValue evaluated = evaluator.evaluate(cell);
            if (evaluated != null
                    && evaluated.getCellType() == CellType.NUMERIC) {
                return NumberToTextConverter.toText(
                        evaluated.getNumberValue()
                );
            }
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    private void validateRequiredAndLength(
            String value,
            String requiredMessage,
            String lengthMessage,
            int maxLength,
            List<String> errors
    ) {
        if (value.isBlank()) {
            errors.add(requiredMessage);
        } else if (value.length() > maxLength) {
            errors.add(lengthMessage);
        }
    }

    private void validateOptionalLength(
            String value,
            String message,
            int maxLength,
            List<String> errors
    ) {
        if (value != null && value.length() > maxLength) {
            errors.add(message);
        }
    }

    private GuardianImportRowErrorResponse fileError(String message) {
        return new GuardianImportRowErrorResponse(
                null,
                null,
                null,
                List.of(message)
        );
    }

    private Boolean parseYesNo(String value) {
        String normalized = canonical(value);
        if (normalized.equals("si")) {
            return true;
        }
        if (normalized.equals("no")) {
            return false;
        }
        return null;
    }

    private String normalizeExternalReference(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeEnrollment(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String canonical(String value) {
        String normalized = Normalizer.normalize(
                normalizeText(value),
                Normalizer.Form.NFD
        );
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private String safeFilePart(String value) {
        String normalized = canonical(value)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "escuela" : normalized;
    }

    private record ImportContext(
            School school,
            List<Student> students,
            List<Guardian> guardians,
            List<StudentGuardian> relationships
    ) {
    }

    private record ParsedRow(
            int rowNumber,
            String externalReference,
            String fullName,
            String phone,
            String email,
            String enrollmentNumber,
            String relationship,
            Boolean primaryContact,
            Boolean receivesNotifications,
            Student student,
            Guardian existingGuardian,
            List<String> errors
    ) {
    }

    private record Analysis(
            List<ParsedRow> rows,
            GuardianImportValidationResponse validation
    ) {
    }

    private record RelationshipKey(Long studentId, Long guardianId) {
    }

    private record FileRelationshipKey(
            String externalReference,
            String enrollmentNumber
    ) {
    }
}
