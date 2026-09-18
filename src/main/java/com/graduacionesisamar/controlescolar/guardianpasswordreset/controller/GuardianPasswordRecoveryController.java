package com.graduacionesisamar.controlescolar.guardianpasswordreset.controller;

import com.graduacionesisamar.controlescolar.guardianpasswordreset.dto.GuardianPasswordResetRequest;
import com.graduacionesisamar.controlescolar.guardianpasswordreset.service.GuardianPasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public guardian password recovery endpoint.
 */
@RestController
@RequestMapping("/api/v1/guardian-auth/password-reset")
@RequiredArgsConstructor
public class GuardianPasswordRecoveryController {

    private final GuardianPasswordRecoveryService recoveryService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void request(
            @Valid @RequestBody GuardianPasswordResetRequest request
    ) {
        recoveryService.request(request)
                .forEach(eventPublisher::publishEvent);
    }
}
