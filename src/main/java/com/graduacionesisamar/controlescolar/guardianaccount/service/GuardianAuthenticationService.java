package com.graduacionesisamar.controlescolar.guardianaccount.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardianaccount.dto.GuardianLoginRequest;
import com.graduacionesisamar.controlescolar.guardianaccount.dto.GuardianLoginResponse;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import com.graduacionesisamar.controlescolar.guardiansession.service.IssuedGuardianSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Recreates a guardian session from recoverable credentials.
 */
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = ResponseStatusException.class)
public class GuardianAuthenticationService {

    private final GuardianAccountService guardianAccountService;
    private final GuardianDeviceService guardianDeviceService;
    private final GuardianSessionService guardianSessionService;

    public CompletedGuardianLogin login(GuardianLoginRequest request) {
        GuardianAccount account = guardianAccountService.authenticate(
                request.schoolCode(),
                request.username(),
                request.password()
        );
        Guardian guardian = account.getGuardian();

        GuardianDeviceResponse device = registerOptionalDevice(
                guardian,
                request.fcmToken(),
                request.deviceName()
        );
        IssuedGuardianSession session = guardianSessionService.issue(
                guardian,
                device == null ? null : device.id(),
                request.deviceName()
        );

        return new CompletedGuardianLogin(
                new GuardianLoginResponse(
                        guardian.getId(),
                        guardian.getFullName(),
                        account.getSchool().getName(),
                        account.getSchool().getCode(),
                        account.getUsername(),
                        device,
                        device != null,
                        session.expiresAt()
                ),
                session.token()
        );
    }

    private GuardianDeviceResponse registerOptionalDevice(
            Guardian guardian,
            String fcmToken,
            String deviceName
    ) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return null;
        }
        return guardianDeviceService.registerFromEnrollment(
                guardian,
                new RegisterGuardianDeviceRequest(
                        fcmToken.trim(),
                        deviceName
                )
        );
    }
}
