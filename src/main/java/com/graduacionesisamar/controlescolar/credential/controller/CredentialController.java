package com.graduacionesisamar.controlescolar.credential.controller;

import com.graduacionesisamar.controlescolar.credential.dto.CredentialResponse;
import com.graduacionesisamar.controlescolar.credential.service.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes REST operations for student QR credentials.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * Creates an active credential for a student.
     */
    @PostMapping("/students/{studentId}/credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse create(@PathVariable Long studentId) {
        return credentialService.create(studentId);
    }

    /**
     * Returns the active credential assigned to a student.
     */
    @GetMapping("/students/{studentId}/credentials/active")
    public CredentialResponse findActive(@PathVariable Long studentId) {
        return credentialService.findActive(studentId);
    }

    /**
     * Deactivates a credential.
     */
    @PatchMapping("/credentials/{credentialId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long credentialId) {
        credentialService.deactivate(credentialId);
    }

    /**
     * Replaces the active credential assigned to a student.
     */
    @PostMapping("/students/{studentId}/credentials/regenerate")
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse regenerate(@PathVariable Long studentId) {
        return credentialService.regenerate(studentId);
    }
}