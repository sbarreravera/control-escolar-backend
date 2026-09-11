package com.graduacionesisamar.controlescolar.communication.controller;

import com.graduacionesisamar.controlescolar.communication.dto.GuardianCommunicationResponse;
import com.graduacionesisamar.controlescolar.communication.service.GuardianCommunicationService;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/guardian/communications")
@RequiredArgsConstructor
@Validated
public class GuardianCommunicationController {

    private final GuardianCommunicationService communicationService;

    @GetMapping
    public List<GuardianCommunicationResponse> findAll(
            Authentication authentication
    ) {
        return communicationService.findAll(requirePrincipal(authentication));
    }

    @PostMapping("/{communicationId}/view")
    public GuardianCommunicationResponse view(
            Authentication authentication,
            @PathVariable @Positive Long communicationId
    ) {
        return communicationService.view(
                requirePrincipal(authentication),
                communicationId
        );
    }

    @PostMapping("/{communicationId}/acknowledge")
    public GuardianCommunicationResponse acknowledge(
            Authentication authentication,
            @PathVariable @Positive Long communicationId
    ) {
        return communicationService.acknowledge(
                requirePrincipal(authentication),
                communicationId
        );
    }

    private GuardianPrincipal requirePrincipal(Authentication authentication) {
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
