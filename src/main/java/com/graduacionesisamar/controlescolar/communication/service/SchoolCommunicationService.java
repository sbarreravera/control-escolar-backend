package com.graduacionesisamar.controlescolar.communication.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.communication.dto.*;
import com.graduacionesisamar.controlescolar.communication.entity.*;
import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRepository;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.notification.entity.NotificationLog;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolCommunicationService {

    private static final int MAX_PUSH_MESSAGE_LENGTH = 500;

    private final SchoolCommunicationRepository communicationRepository;
    private final SchoolCommunicationRecipientRepository recipientRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianDeviceRepository guardianDeviceRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SchoolRepository schoolRepository;
    private final AppUserRepository appUserRepository;
    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public CommunicationAudiencePreviewResponse preview(
            CommunicationAudienceRequest request
    ) {
        ResolvedAudience audience = resolveAudience(request);
        Set<Long> guardiansWithPush = activePushGuardianIds(audience.guardians());

        return new CommunicationAudiencePreviewResponse(
                audience.students().size(),
                audience.guardians().size(),
                guardiansWithPush.size(),
                audience.guardians().size() - guardiansWithPush.size(),
                audience.summary()
        );
    }

    public CommunicationResponse create(CreateCommunicationRequest request) {
        ResolvedAudience audience = resolveAudience(request.audience());
        requireRecipients(audience);

        OffsetDateTime now = OffsetDateTime.now();
        boolean scheduled = request.scheduledAt() != null
                && request.scheduledAt().isAfter(now);

        SchoolCommunication communication = new SchoolCommunication();
        communication.setSchool(audience.school());
        communication.setCreatedBy(requireCurrentUser());
        applyContent(communication, request, audience);
        communication.setStatus(
                scheduled
                        ? CommunicationStatus.SCHEDULED
                        : CommunicationStatus.PUBLISHED
        );
        communication.setScheduledAt(scheduled ? request.scheduledAt() : null);
        communication.setPublishedAt(scheduled ? null : now);
        communication.setPushRecipientCount(
                activePushGuardianIds(audience.guardians()).size()
        );
        communicationRepository.save(communication);

        List<SchoolCommunicationRecipient> recipients = createRecipients(
                communication,
                audience.guardians()
        );
        recipientRepository.saveAll(recipients);

        if (!scheduled) {
            publishPushes(communication, recipients, now);
        }

        return toResponse(communication);
    }

    public CommunicationResponse updateScheduled(
            Long communicationId,
            CreateCommunicationRequest request
    ) {
        SchoolCommunication communication = requireCommunication(communicationId);
        if (communication.getStatus() != CommunicationStatus.SCHEDULED) {
            throw conflict("Sólo los avisos programados pueden editarse.");
        }
        if (request.scheduledAt() == null
                || !request.scheduledAt().isAfter(OffsetDateTime.now())) {
            throw badRequest("La nueva fecha programada debe estar en el futuro.");
        }
        if (!Objects.equals(
                communication.getSchool().getId(),
                request.audience().schoolId()
        )) {
            throw badRequest("La escuela del aviso no puede cambiarse.");
        }

        ResolvedAudience audience = resolveAudience(request.audience());
        requireRecipients(audience);
        applyContent(communication, request, audience);
        communication.setScheduledAt(request.scheduledAt());
        communication.setPushRecipientCount(
                activePushGuardianIds(audience.guardians()).size()
        );

        recipientRepository.deleteAllByCommunication_Id(communicationId);
        recipientRepository.flush();
        recipientRepository.saveAll(createRecipients(
                communication,
                audience.guardians()
        ));

        communicationRepository.save(communication);
        return toResponse(communication);
    }

    public CommunicationResponse cancel(Long communicationId) {
        SchoolCommunication communication = requireCommunication(communicationId);
        if (communication.getStatus() != CommunicationStatus.SCHEDULED) {
            throw conflict("Sólo los avisos programados pueden cancelarse.");
        }

        communication.setStatus(CommunicationStatus.CANCELLED);
        communication.setScheduledAt(null);
        communicationRepository.save(communication);
        return toResponse(communication);
    }

    @Transactional(readOnly = true)
    public List<CommunicationResponse> findHistory(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        return communicationRepository
                .findTop100BySchool_IdOrderByCreatedAtDesc(schoolId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunicationResponse findOne(Long communicationId) {
        return toResponse(requireCommunication(communicationId));
    }

    @Transactional(readOnly = true)
    public List<CommunicationRecipientResponse> findRecipients(
            Long communicationId
    ) {
        SchoolCommunication communication = requireCommunication(communicationId);
        return recipientRepository
                .findAllByCommunication_IdOrderByGuardian_FullNameAsc(
                        communication.getId()
                )
                .stream()
                .map(this::toRecipientResponse)
                .toList();
    }

    public int publishDue() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SchoolCommunication> due = communicationRepository
                .findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        CommunicationStatus.SCHEDULED,
                        now
                );

        for (SchoolCommunication communication : due) {
            List<SchoolCommunicationRecipient> recipients = recipientRepository
                    .findAllByCommunication_IdOrderByGuardian_FullNameAsc(
                            communication.getId()
                    );
            communication.setStatus(CommunicationStatus.PUBLISHED);
            communication.setPublishedAt(now);
            communication.setScheduledAt(null);
            publishPushes(communication, recipients, now);
            communicationRepository.save(communication);
        }

        return due.size();
    }

    private void publishPushes(
            SchoolCommunication communication,
            List<SchoolCommunicationRecipient> recipients,
            OffsetDateTime publishedAt
    ) {
        Set<Long> activePushGuardians = activePushGuardianIds(
                recipients.stream()
                        .map(SchoolCommunicationRecipient::getGuardian)
                        .toList()
        );

        List<NotificationLog> logs = new ArrayList<>();
        int pushRecipients = 0;

        for (SchoolCommunicationRecipient recipient : recipients) {
            recipient.setPushSentAt(null);
            recipient.setPushError(null);

            if (!activePushGuardians.contains(recipient.getGuardian().getId())) {
                recipient.setPushStatus(
                        CommunicationRecipientPushStatus.NOT_ENABLED
                );
                continue;
            }

            pushRecipients++;
            recipient.setPushStatus(CommunicationRecipientPushStatus.PENDING);

            NotificationLog log = new NotificationLog();
            log.setCommunicationRecipient(recipient);
            log.setGuardian(recipient.getGuardian());
            log.setTitle(communication.getTitle());
            log.setMessage(toPushMessage(communication.getMessage()));
            logs.add(log);
        }

        communication.setPushRecipientCount(pushRecipients);
        if (communication.getPublishedAt() == null) {
            communication.setPublishedAt(publishedAt);
        }
        recipientRepository.saveAll(recipients);
        notificationLogRepository.saveAll(logs);
    }

    private void applyContent(
            SchoolCommunication communication,
            CreateCommunicationRequest request,
            ResolvedAudience audience
    ) {
        communication.setType(request.type());
        communication.setCategory(request.category());
        communication.setPriority(request.priority());
        communication.setTitle(request.title().trim());
        communication.setMessage(request.message().trim());
        communication.setRequiresAcknowledgement(
                request.requiresAcknowledgement()
        );
        communication.setAudienceType(request.audience().audienceType());
        communication.setAudienceSummary(audience.summary());
        communication.setStudentCount(audience.students().size());
        communication.setRecipientCount(audience.guardians().size());
    }

    private ResolvedAudience resolveAudience(
            CommunicationAudienceRequest request
    ) {
        schoolAccessService.requireAccessToSchool(request.schoolId());
        School school = schoolRepository.findById(request.schoolId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Escuela no encontrada."
                ));

        validateAudienceRequest(request);
        List<Student> schoolStudents = studentRepository
                .findCommunicationAudienceStudents(request.schoolId());

        Predicate<Student> selector = buildStudentSelector(request);
        List<Student> selectedStudents = schoolStudents.stream()
                .filter(selector)
                .toList();
        Set<Long> selectedStudentIds = selectedStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toSet());

        Map<Long, Guardian> guardians = new LinkedHashMap<>();
        if (!selectedStudentIds.isEmpty()) {
            for (StudentGuardian link : studentGuardianRepository
                    .findCommunicationCandidates(request.schoolId())) {
                if (selectedStudentIds.contains(link.getStudent().getId())) {
                    guardians.putIfAbsent(
                            link.getGuardian().getId(),
                            link.getGuardian()
                    );
                }
            }
        }

        return new ResolvedAudience(
                school,
                selectedStudents,
                new ArrayList<>(guardians.values()),
                buildAudienceSummary(request, selectedStudents)
        );
    }

    private Predicate<Student> buildStudentSelector(
            CommunicationAudienceRequest request
    ) {
        return switch (request.audienceType()) {
            case ALL_SCHOOL -> student -> true;
            case CYCLE -> student -> student.getSchoolGroup() != null
                    && student.getSchoolGroup().getAcademicCycle() != null
                    && Objects.equals(
                    student.getSchoolGroup().getAcademicCycle().getId(),
                    request.academicCycleId()
            );
            case GRADES -> {
                Set<String> grades = request.gradeNames().stream()
                        .filter(Objects::nonNull)
                        .map(this::normalize)
                        .collect(Collectors.toSet());
                yield student -> student.getSchoolGroup() != null
                        && student.getSchoolGroup().getAcademicCycle() != null
                        && Objects.equals(
                        student.getSchoolGroup().getAcademicCycle().getId(),
                        request.academicCycleId()
                )
                        && grades.contains(normalize(
                        student.getSchoolGroup().getGradeName()
                ));
            }
            case GROUPS -> {
                Set<Long> groupIds = new HashSet<>(request.schoolGroupIds());
                yield student -> student.getSchoolGroup() != null
                        && groupIds.contains(student.getSchoolGroup().getId());
            }
            case STUDENTS -> {
                Set<Long> studentIds = new HashSet<>(request.studentIds());
                yield student -> studentIds.contains(student.getId());
            }
        };
    }

    private void validateAudienceRequest(CommunicationAudienceRequest request) {
        switch (request.audienceType()) {
            case ALL_SCHOOL -> {
            }
            case CYCLE -> requireValue(
                    request.academicCycleId() != null,
                    "Selecciona un ciclo escolar."
            );
            case GRADES -> {
                requireValue(
                        request.academicCycleId() != null,
                        "Selecciona un ciclo escolar."
                );
                requireValue(
                        request.gradeNames() != null
                                && !request.gradeNames().isEmpty(),
                        "Selecciona al menos un grado."
                );
            }
            case GROUPS -> requireValue(
                    request.schoolGroupIds() != null
                            && !request.schoolGroupIds().isEmpty(),
                    "Selecciona al menos un grupo."
            );
            case STUDENTS -> requireValue(
                    request.studentIds() != null
                            && !request.studentIds().isEmpty(),
                    "Selecciona al menos un alumno."
            );
        }
    }

    private String buildAudienceSummary(
            CommunicationAudienceRequest request,
            List<Student> students
    ) {
        return switch (request.audienceType()) {
            case ALL_SCHOOL -> "Toda la escuela";
            case CYCLE -> students.stream()
                    .map(Student::getSchoolGroup)
                    .filter(Objects::nonNull)
                    .map(group -> group.getAcademicCycle().getName())
                    .findFirst()
                    .map(name -> "Ciclo " + name)
                    .orElse("Ciclo seleccionado");
            case GRADES -> "Grados: " + String.join(
                    ", ",
                    request.gradeNames()
            );
            case GROUPS -> "%d grupo(s) seleccionado(s)".formatted(
                    request.schoolGroupIds().size()
            );
            case STUDENTS -> "%d alumno(s) seleccionado(s)".formatted(
                    request.studentIds().size()
            );
        };
    }

    private Set<Long> activePushGuardianIds(List<Guardian> guardians) {
        if (guardians.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = guardians.stream()
                .map(Guardian::getId)
                .toList();
        return guardianDeviceRepository
                .findAllByGuardian_IdInAndActiveTrue(ids)
                .stream()
                .map(device -> device.getGuardian().getId())
                .collect(Collectors.toSet());
    }

    private List<SchoolCommunicationRecipient> createRecipients(
            SchoolCommunication communication,
            List<Guardian> guardians
    ) {
        return guardians.stream().map(guardian -> {
            SchoolCommunicationRecipient recipient =
                    new SchoolCommunicationRecipient();
            recipient.setCommunication(communication);
            recipient.setGuardian(guardian);
            return recipient;
        }).toList();
    }

    private SchoolCommunication requireCommunication(Long communicationId) {
        SchoolCommunication communication = communicationRepository
                .findById(communicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aviso no encontrado."
                ));
        schoolAccessService.requireAccessToSchool(
                communication.getSchool().getId()
        );
        return communication;
    }

    private AppUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Se requiere iniciar sesión."
            );
        }
        return appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado."
                ));
    }

    private CommunicationResponse toResponse(
            SchoolCommunication communication
    ) {
        Long id = communication.getId();
        return new CommunicationResponse(
                id,
                communication.getSchool().getId(),
                communication.getCreatedBy().getFullName(),
                communication.getType(),
                communication.getCategory(),
                communication.getPriority(),
                communication.getStatus(),
                communication.getAudienceType(),
                communication.getAudienceSummary(),
                communication.getTitle(),
                communication.getMessage(),
                Boolean.TRUE.equals(communication.getRequiresAcknowledgement()),
                communication.getStudentCount(),
                communication.getRecipientCount(),
                communication.getPushRecipientCount(),
                recipientRepository.countByCommunication_IdAndPushStatus(
                        id,
                        CommunicationRecipientPushStatus.SENT
                ),
                recipientRepository.countByCommunication_IdAndPushStatus(
                        id,
                        CommunicationRecipientPushStatus.FAILED
                ),
                recipientRepository.countByCommunication_IdAndViewedAtIsNotNull(id),
                recipientRepository.countByCommunication_IdAndAcknowledgedAtIsNotNull(id),
                communication.getScheduledAt(),
                communication.getPublishedAt(),
                communication.getCreatedAt()
        );
    }

    private CommunicationRecipientResponse toRecipientResponse(
            SchoolCommunicationRecipient recipient
    ) {
        Guardian guardian = recipient.getGuardian();
        return new CommunicationRecipientResponse(
                recipient.getId(),
                guardian.getId(),
                guardian.getFullName(),
                guardian.getPhone(),
                guardian.getEmail(),
                recipient.getPushStatus(),
                recipient.getPushSentAt(),
                recipient.getViewedAt(),
                recipient.getAcknowledgedAt()
        );
    }

    private String toPushMessage(String message) {
        if (message.length() <= MAX_PUSH_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_PUSH_MESSAGE_LENGTH - 1) + "…";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void requireRecipients(ResolvedAudience audience) {
        if (audience.students().isEmpty()) {
            throw badRequest("La selección no contiene alumnos activos.");
        }
        if (audience.guardians().isEmpty()) {
            throw badRequest(
                    "La selección no contiene tutores habilitados para recibir avisos."
            );
        }
    }

    private void requireValue(boolean valid, String message) {
        if (!valid) {
            throw badRequest(message);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record ResolvedAudience(
            School school,
            List<Student> students,
            List<Guardian> guardians,
            String summary
    ) {
    }
}
