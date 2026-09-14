package com.graduacionesisamar.controlescolar.guardianregistration.controller;

import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianRegistrationContextResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianRegistrationSchoolResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianSelfRegistrationRequest;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianSelfRegistrationResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.service.GuardianRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/guardian-registration")
@RequiredArgsConstructor
public class GuardianRegistrationController {

    private final GuardianRegistrationService guardianRegistrationService;

    @GetMapping("/schools")
    public List<GuardianRegistrationSchoolResponse> listSchools() {
        return guardianRegistrationService.listAvailableSchools();
    }

    @GetMapping("/context")
    public GuardianRegistrationContextResponse context(
            @RequestParam String token
    ) {
        return guardianRegistrationService.resolveContext(token);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianSelfRegistrationResponse register(
            @Valid @RequestBody GuardianSelfRegistrationRequest request
    ) {
        return guardianRegistrationService.register(request);
    }
}
