package com.graduacionesisamar.controlescolar.guardiansession.controller;

import com.graduacionesisamar.controlescolar.guardiansession.dto.GuardianIdentityResponse;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianSessionCookieService;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Session and identity endpoints that never accept an arbitrary guardian id.
 */
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor
public class GuardianIdentityController {

    private final GuardianSessionCookieService cookieService;
    private final GuardianSessionService guardianSessionService;
    private final GuardianAccountService guardianAccountService;
    private final GuardianDeviceRepository guardianDeviceRepository;

    @GetMapping("/me")
    public GuardianIdentityResponse me(Authentication authentication) {
        GuardianPrincipal principal = requirePrincipal(authentication);
        var account = guardianAccountService.findByGuardianId(
                principal.guardianId()
        );
        boolean notificationsEnabled = principal.guardianDeviceId() != null
                && guardianDeviceRepository
                .existsByIdAndGuardian_IdAndActiveTrue(
                        principal.guardianDeviceId(),
                        principal.guardianId()
                );

        return new GuardianIdentityResponse(
                principal.guardianId(),
                principal.schoolId(),
                principal.guardianName(),
                principal.schoolName(),
                account.getSchool().getCode(),
                account.getUsername(),
                notificationsEnabled,
                principal.expiresAt()
        );
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        cookieService.read(request)
                .ifPresent(guardianSessionService::revoke);
        cookieService.clear(response);
    }

    private GuardianPrincipal requirePrincipal(
            Authentication authentication
    ) {
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof GuardianPrincipal principal)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Guardian session is required"
            );
        }

        return principal;
    }
}
