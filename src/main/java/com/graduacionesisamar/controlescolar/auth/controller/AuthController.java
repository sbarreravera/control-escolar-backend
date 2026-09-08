package com.graduacionesisamar.controlescolar.auth.controller;

import com.graduacionesisamar.controlescolar.auth.dto.AuthenticatedUserResponse;
import com.graduacionesisamar.controlescolar.auth.dto.CsrfTokenResponse;
import com.graduacionesisamar.controlescolar.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(
            Authentication authentication
    ) {
        return authService.getAuthenticatedUser(
                authentication.getName()
        );
    }
}