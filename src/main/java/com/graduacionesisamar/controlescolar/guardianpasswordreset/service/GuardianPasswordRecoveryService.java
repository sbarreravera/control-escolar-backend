package com.graduacionesisamar.controlescolar.guardianpasswordreset.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.repository.GuardianAccountRepository;
import com.graduacionesisamar.controlescolar.guardianpasswordreset.dto.GuardianPasswordResetRequest;
import com.graduacionesisamar.controlescolar.guardianpasswordreset.event.GuardianPasswordResetRequestedEvent;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service.GuardianDeviceEnrollmentService;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a guardian by school and registered email without exposing whether
 * an account exists. Only already-activated guardian accounts can be reset.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianPasswordRecoveryService {

    private final SchoolRepository schoolRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianAccountRepository guardianAccountRepository;
    private final GuardianDeviceEnrollmentService enrollmentService;

    public List<GuardianPasswordResetRequestedEvent> request(
            GuardianPasswordResetRequest request
    ) {
        School school = schoolRepository
                .findByCodeIgnoreCase(request.schoolCode().trim())
                .orElse(null);

        if (school == null || !Boolean.TRUE.equals(school.getActive())) {
            return List.of();
        }

        List<Guardian> guardians = guardianRepository
                .findAllBySchool_IdAndEmailIgnoreCase(
                        school.getId(),
                        request.email().trim()
                );

        List<GuardianPasswordResetRequestedEvent> events =
                new ArrayList<>();

        for (Guardian guardian : guardians) {
            if (!Boolean.TRUE.equals(guardian.getActive())) {
                continue;
            }

            GuardianAccount account = guardianAccountRepository
                    .findByGuardian_Id(guardian.getId())
                    .orElse(null);

            if (account == null
                    || !Boolean.TRUE.equals(account.getActive())
                    || !account.isActivated()) {
                continue;
            }

            enrollmentService
                    .createSelfServicePasswordReset(guardian, account)
                    .ifPresent(invitation -> events.add(
                            new GuardianPasswordResetRequestedEvent(
                                    guardian.getEmail().trim(),
                                    invitation.guardianName(),
                                    invitation.schoolName(),
                                    invitation.schoolCode(),
                                    invitation.username(),
                                    invitation.enrollmentToken(),
                                    invitation.expiresAt()
                            )
                    ));
        }

        return List.copyOf(events);
    }
}
