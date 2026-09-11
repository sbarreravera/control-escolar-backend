package com.graduacionesisamar.controlescolar.guardianaccount.controller;

import com.graduacionesisamar.controlescolar.guardianaccount.dto.GuardianLoginRequest;
import com.graduacionesisamar.controlescolar.guardianaccount.dto.GuardianLoginResponse;
import com.graduacionesisamar.controlescolar.guardianaccount.service.CompletedGuardianLogin;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAuthenticationService;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianSessionCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public guardian credential login.
 */
@RestController
@RequestMapping("/api/v1/guardian-auth")
@RequiredArgsConstructor
public class GuardianAuthenticationController {

    private final GuardianAuthenticationService authenticationService;
    private final GuardianSessionCookieService cookieService;

    @PostMapping("/login")
    public GuardianLoginResponse login(
            @Valid @RequestBody GuardianLoginRequest request,
            HttpServletResponse response
    ) {
        CompletedGuardianLogin completed = authenticationService.login(
                request
        );
        cookieService.write(
                response,
                completed.sessionToken(),
                completed.response().sessionExpiresAt()
        );
        return completed.response();
    }
}
