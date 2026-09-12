package com.graduacionesisamar.controlescolar.guardian.service;

import com.graduacionesisamar.controlescolar.communication.repository.SchoolCommunicationRecipientRepository;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianDeletionImpactResponse;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianDeletionStudentResponse;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.notification.repository.NotificationLogRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class GuardianDeletionService {

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianDeviceRepository guardianDeviceRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SchoolCommunicationRecipientRepository communicationRecipientRepository;
    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public GuardianDeletionImpactResponse preview(Long guardianId) {
        Guardian guardian = findGuardian(guardianId);
        schoolAccessService.requireAccessToSchool(guardian.getSchool().getId());

        var students = studentGuardianRepository
                .findAllByGuardian_IdOrderByStudent_LastNameAscStudent_FirstNameAsc(
                        guardianId
                )
                .stream()
                .map(link -> {
                    var student = link.getStudent();
                    return new GuardianDeletionStudentResponse(
                            student.getId(),
                            student.getFirstName() + " " + student.getLastName(),
                            student.getEnrollmentNumber()
                    );
                })
                .toList();

        return new GuardianDeletionImpactResponse(
                guardian.getId(),
                guardian.getFullName(),
                guardian.getExternalReference(),
                students,
                guardianDeviceRepository.countByGuardian_IdAndActiveTrue(guardianId),
                notificationLogRepository.countByGuardian_Id(guardianId),
                communicationRecipientRepository.countByGuardian_Id(guardianId)
        );
    }

    public void delete(Long guardianId) {
        Guardian guardian = findGuardian(guardianId);
        schoolAccessService.requireAccessToSchool(guardian.getSchool().getId());

        notificationLogRepository.deleteAllForGuardian(guardianId);
        communicationRecipientRepository.deleteAllForGuardian(guardianId);
        guardianRepository.delete(guardian);
        guardianRepository.flush();
    }

    private Guardian findGuardian(Long guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian not found"
                ));
    }
}
