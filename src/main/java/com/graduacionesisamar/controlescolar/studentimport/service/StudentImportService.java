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
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportRowErrorResponse;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportTemplate;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportValidationResponse;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates, validates and imports the official initial student workbook.
 */
@Service
@RequiredArgsConstructor
public class StudentImportService {

    static final String STUDENTS_SHEET = "Alumnos";
    static final String CATALOG_SHEET = "Catálogos";
    static final String INSTRUCTIONS_SHEET = "Instrucciones";
    static final String GROUP_SEPARATOR = " | ";

    private static final String[] HEADERS = {
            "Matrícula",
            "Nombre(s)",
            "Apellidos",
            "Grado y grupo"
    };

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_DATA_ROWS = 2000;

    private final AcademicCycleRepository academicCycleRepository;
    private final SchoolGroupRepository schoolGroupRepository;
    private final StudentRepository studentRepository;
    private final SchoolAccessService schoolAccessService;

    /**
     * Generates a workbook tied to the authenticated user's school and cycle.
     */
    @Transactional(readOnly = true)
    public StudentImportTemplate generateTemplate(Long academicCycleId) {
        ImportContext context = loadContext(academicCycleId);

        if (context.groups().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The academic cycle has no active school groups"
            );
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            createInstructionsSheet(workbook, context);
            createCatalogSheet(workbook, context.groups());
            createStudentsSheet(workbook, context.groups());
            workbook.setSheetOrder(STUDENTS_SHEET, 1);
            workbook.setActiveSheet(1);
            workbook.write(output);

            String cyclePart = safeFilePart(context.cycle().getName());
            return new StudentImportTemplate(
                    "carga-inicial-alumnos-" + cyclePart + ".xlsx",
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not generate the student import template",
                    exception
            );
        }
    }

    /**
     * Validates the workbook without changing data.
     */
    @Transactional(readOnly = true)
    public StudentImportValidationResponse validate(
            Long academicCycleId,
            MultipartFile file
    ) {
        ImportContext context = loadContext(academicCycleId);
        return analyze(context, file).validation();
    }

    /**
     * Imports every row atomically after repeating all validations.
     */
    @Transactional
    public StudentImportResultResponse importStudents(
            Long academicCycleId,
            MultipartFile file
    ) {
        ImportContext context = loadContext(academicCycleId);
        Analysis analysis = analyze(context, file);

        if (!analysis.validation().canImport()) {
            return new StudentImportResultResponse(
                    0,
                    analysis.validation()
            );
        }

        School school = context.cycle().getSchool();
        List<Student> students = analysis.rows().stream()
                .map(row -> buildStudent(school, row))
                .toList();

        studentRepository.saveAll(students);

        return new StudentImportResultResponse(
                students.size(),
                analysis.validation()
        );
    }

    private ImportContext loadContext(Long academicCycleId) {
        AcademicCycle cycle = academicCycleRepository
                .findById(academicCycleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Academic cycle not found"
                ));

        schoolAccessService.requireAccessToSchool(
                cycle.getSchool().getId()
        );

        if (!Boolean.TRUE.equals(cycle.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Academic cycle is not active"
            );
        }

        List<SchoolGroup> groups = schoolGroupRepository
                .findAllByAcademicCycle_IdOrderByGradeNameAscGroupNameAsc(
                        academicCycleId
                )
                .stream()
                .filter(group -> Boolean.TRUE.equals(group.getActive()))
                .toList();

        return new ImportContext(cycle, groups);
    }

    private Analysis analyze(
            ImportContext context,
            MultipartFile file
    ) {
        List<StudentImportRowErrorResponse> fileErrors =
                validateFile(file);

        if (!fileErrors.isEmpty()) {
            return emptyAnalysis(fileErrors);
        }

        try (InputStream input = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet(STUDENTS_SHEET);

            if (sheet == null) {
                return emptyAnalysis(List.of(fileError(
                        "No se encontró la hoja obligatoria \"Alumnos\"."
                )));
            }

            List<StudentImportRowErrorResponse> headerErrors =
                    validateHeaders(sheet);

            if (!headerErrors.isEmpty()) {
                return emptyAnalysis(headerErrors);
            }

            if (sheet.getLastRowNum() > MAX_DATA_ROWS) {
                return emptyAnalysis(List.of(fileError(
                        "El archivo supera el máximo de "
                                + MAX_DATA_ROWS
                                + " alumnos."
                )));
            }

            Map<String, SchoolGroup> groupsByLabel = context.groups()
                    .stream()
                    .collect(Collectors.toMap(
                            group -> normalizeKey(groupLabel(group)),
                            Function.identity(),
                            (first, second) -> first,
                            LinkedHashMap::new
                    ));

            Set<String> existingEnrollments = studentRepository
                    .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(
                            context.cycle().getSchool().getId()
                    )
                    .stream()
                    .map(Student::getEnrollmentNumber)
                    .map(this::normalizeEnrollment)
                    .collect(Collectors.toSet());

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
                        groupsByLabel,
                        existingEnrollments,
                        formatter,
                        evaluator
                ));
            }

            addWorkbookDuplicateErrors(rows);
            return buildAnalysis(rows);
        } catch (IOException | RuntimeException exception) {
            return emptyAnalysis(List.of(fileError(
                    "No fue posible leer el archivo. Descarga nuevamente el formato oficial."
            )));
        }
    }

    private List<StudentImportRowErrorResponse> validateFile(
            MultipartFile file
    ) {
        List<StudentImportRowErrorResponse> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add(fileError("Selecciona un archivo de Excel."));
            return errors;
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            errors.add(fileError(
                    "El archivo debe tener extensión .xlsx."
            ));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            errors.add(fileError(
                    "El archivo no puede superar 5 MB."
            ));
        }

        return errors;
    }

    private List<StudentImportRowErrorResponse> validateHeaders(
            Sheet sheet
    ) {
        Row header = sheet.getRow(0);
        if (header == null) {
            return List.of(fileError(
                    "La hoja \"Alumnos\" no contiene encabezados."
            ));
        }

        DataFormatter formatter = new DataFormatter();
        List<String> messages = new ArrayList<>();

        for (int column = 0; column < HEADERS.length; column++) {
            String actual = formatter.formatCellValue(
                    header.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
            );

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

        if (messages.isEmpty()) {
            return List.of();
        }

        return List.of(new StudentImportRowErrorResponse(
                1,
                null,
                messages
        ));
    }

    private ParsedRow parseRow(
            Row row,
            int rowNumber,
            Map<String, SchoolGroup> groupsByLabel,
            Set<String> existingEnrollments,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        String enrollment = normalizeEnrollment(
                identifierCellValue(row, 0, formatter, evaluator)
        );
        String firstName = normalizeText(
                cellValue(row, 1, formatter, evaluator)
        );
        String lastName = normalizeText(
                cellValue(row, 2, formatter, evaluator)
        );
        String groupValue = normalizeText(
                cellValue(row, 3, formatter, evaluator)
        );

        List<String> errors = new ArrayList<>();
        validateRequiredAndLength(
                enrollment,
                "La matrícula es obligatoria.",
                "La matrícula no puede superar 50 caracteres.",
                50,
                errors
        );
        validateRequiredAndLength(
                firstName,
                "El nombre es obligatorio.",
                "El nombre no puede superar 100 caracteres.",
                100,
                errors
        );
        validateRequiredAndLength(
                lastName,
                "Los apellidos son obligatorios.",
                "Los apellidos no pueden superar 150 caracteres.",
                150,
                errors
        );
        validateRequiredAndLength(
                groupValue,
                "El grado y grupo es obligatorio.",
                "El grado y grupo no puede superar 103 caracteres.",
                103,
                errors
        );

        if (!enrollment.isBlank()
                && existingEnrollments.contains(enrollment)) {
            errors.add(
                    "La matrícula ya está registrada en esta escuela."
            );
        }

        SchoolGroup schoolGroup = groupsByLabel.get(
                normalizeKey(groupValue)
        );

        if (!groupValue.isBlank() && schoolGroup == null) {
            errors.add(
                    "El grado y grupo no pertenece al ciclo seleccionado."
            );
        }

        return new ParsedRow(
                rowNumber,
                enrollment,
                firstName,
                lastName,
                schoolGroup,
                errors
        );
    }

    private void addWorkbookDuplicateErrors(List<ParsedRow> rows) {
        Map<String, Integer> counts = new HashMap<>();

        rows.stream()
                .map(ParsedRow::enrollment)
                .filter(value -> !value.isBlank())
                .forEach(value -> counts.merge(value, 1, Integer::sum));

        rows.stream()
                .filter(row -> !row.enrollment().isBlank())
                .filter(row -> counts.getOrDefault(
                        row.enrollment(),
                        0
                ) > 1)
                .forEach(row -> row.errors().add(
                        "La matrícula está repetida dentro del archivo."
                ));
    }

    private Analysis buildAnalysis(List<ParsedRow> rows) {
        List<StudentImportRowErrorResponse> errors = rows.stream()
                .filter(row -> !row.errors().isEmpty())
                .map(row -> new StudentImportRowErrorResponse(
                        row.rowNumber(),
                        row.enrollment().isBlank()
                                ? null
                                : row.enrollment(),
                        List.copyOf(row.errors())
                ))
                .toList();

        int totalRows = rows.size();
        int invalidRows = errors.size();
        int validRows = totalRows - invalidRows;

        if (totalRows == 0) {
            errors = List.of(fileError(
                    "El archivo no contiene alumnos para importar."
            ));
        }

        StudentImportValidationResponse validation =
                new StudentImportValidationResponse(
                        totalRows,
                        validRows,
                        invalidRows,
                        totalRows > 0 && invalidRows == 0,
                        errors
                );

        return new Analysis(rows, validation);
    }

    private Analysis emptyAnalysis(
            List<StudentImportRowErrorResponse> errors
    ) {
        return new Analysis(
                List.of(),
                new StudentImportValidationResponse(
                        0,
                        0,
                        0,
                        false,
                        errors
                )
        );
    }

    private Student buildStudent(School school, ParsedRow row) {
        SchoolGroup group = row.schoolGroup();
        Student student = new Student();
        student.setSchool(school);
        student.setEnrollmentNumber(row.enrollment());
        student.setFirstName(row.firstName());
        student.setLastName(row.lastName());
        student.setSchoolGroup(group);
        student.setGradeName(group.getGradeName());
        student.setGroupName(group.getGroupName());
        return student;
    }

    private void createInstructionsSheet(
            Workbook workbook,
            ImportContext context
    ) {
        Sheet sheet = workbook.createSheet(INSTRUCTIONS_SHEET);
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle labelStyle = labelStyle(workbook);
        CellStyle wrapStyle = wrapStyle(workbook);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(28);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Carga inicial de alumnos");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        createLabelValueRow(
                sheet,
                2,
                "Escuela",
                context.cycle().getSchool().getName(),
                labelStyle
        );
        createLabelValueRow(
                sheet,
                3,
                "Ciclo escolar",
                context.cycle().getName(),
                labelStyle
        );

        Row instructionsTitle = sheet.createRow(5);
        instructionsTitle.createCell(0).setCellValue("Instrucciones");
        instructionsTitle.getCell(0).setCellStyle(labelStyle);

        String[] instructions = {
                "1. Captura los alumnos únicamente en la hoja \"Alumnos\".",
                "2. No cambies los nombres ni el orden de las columnas.",
                "3. Conserva la matrícula como texto y no la repitas.",
                "4. Selecciona el grado y grupo desde la lista del archivo.",
                "5. No agregues alumnos de otra escuela o ciclo escolar.",
                "6. Guarda el archivo con extensión .xlsx antes de subirlo.",
                "7. El sistema validará todas las filas antes de guardar."
        };

        for (int index = 0; index < instructions.length; index++) {
            Row row = sheet.createRow(6 + index);
            Cell cell = row.createCell(0);
            cell.setCellValue(instructions[index]);
            cell.setCellStyle(wrapStyle);
            sheet.addMergedRegion(
                    new CellRangeAddress(6 + index, 6 + index, 0, 2)
            );
        }

        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 34 * 256);
        sheet.setColumnWidth(2, 20 * 256);
    }

    private void createStudentsSheet(
            Workbook workbook,
            List<SchoolGroup> groups
    ) {
        Sheet sheet = workbook.createSheet(STUDENTS_SHEET);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle textStyle = textStyle(workbook);

        Row header = sheet.createRow(0);
        header.setHeightInPoints(24);
        for (int index = 0; index < HEADERS.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(headerStyle);
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, 3));
        sheet.setDefaultColumnStyle(0, textStyle);
        sheet.setDefaultColumnStyle(1, textStyle);
        sheet.setDefaultColumnStyle(2, textStyle);
        sheet.setDefaultColumnStyle(3, textStyle);

        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 28 * 256);
        sheet.setColumnWidth(2, 36 * 256);
        sheet.setColumnWidth(3, 38 * 256);

        Name groupList = workbook.createName();
        groupList.setNameName("GRUPOS_VALIDOS");
        groupList.setRefersToFormula(
                "'" + CATALOG_SHEET + "'!$A$2:$A$" + (groups.size() + 1)
        );

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper
                .createFormulaListConstraint("GRUPOS_VALIDOS");
        CellRangeAddressList range = new CellRangeAddressList(
                1,
                MAX_DATA_ROWS,
                3,
                3
        );
        DataValidation validation = helper.createValidation(
                constraint,
                range
        );
        validation.setEmptyCellAllowed(false);
        validation.setShowErrorBox(true);
        validation.createErrorBox(
                "Grupo no válido",
                "Selecciona un grado y grupo de la lista."
        );
        validation.setShowPromptBox(true);
        validation.createPromptBox(
                "Grado y grupo",
                "Selecciona el grupo real del ciclo escolar."
        );
        sheet.addValidationData(validation);
    }

    private void createCatalogSheet(
            Workbook workbook,
            List<SchoolGroup> groups
    ) {
        Sheet sheet = workbook.createSheet(CATALOG_SHEET);
        CellStyle headerStyle = headerStyle(workbook);

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Grados y grupos válidos");
        header.getCell(0).setCellStyle(headerStyle);

        for (int index = 0; index < groups.size(); index++) {
            sheet.createRow(index + 1)
                    .createCell(0)
                    .setCellValue(groupLabel(groups.get(index)));
        }

        sheet.setColumnWidth(0, 42 * 256);
        sheet.createFreezePane(0, 1);
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
        sheet.addMergedRegion(new CellRangeAddress(
                rowNumber,
                rowNumber,
                1,
                2
        ));
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
            return NumberToTextConverter.toText(
                    cell.getNumericCellValue()
            );
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

    private StudentImportRowErrorResponse fileError(String message) {
        return new StudentImportRowErrorResponse(
                null,
                null,
                List.of(message)
        );
    }

    private String groupLabel(SchoolGroup group) {
        return normalizeText(group.getGradeName())
                + GROUP_SEPARATOR
                + normalizeText(group.getGroupName());
    }

    private String normalizeEnrollment(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeKey(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String canonical(String value) {
        String normalized = Normalizer.normalize(
                normalizeText(value),
                Normalizer.Form.NFD
        );
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String safeFilePart(String value) {
        String normalized = canonical(value)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "ciclo" : normalized;
    }

    private record ImportContext(
            AcademicCycle cycle,
            List<SchoolGroup> groups
    ) {
    }

    private record ParsedRow(
            int rowNumber,
            String enrollment,
            String firstName,
            String lastName,
            SchoolGroup schoolGroup,
            List<String> errors
    ) {
    }

    private record Analysis(
            List<ParsedRow> rows,
            StudentImportValidationResponse validation
    ) {
    }
}
