package com.graduacionesisamar.controlescolar.guardianportal.controller;

import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianAccessEventPageResponse;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianAccessEventResponse;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianStudentResponse;
import com.graduacionesisamar.controlescolar.guardianportal.service.GuardianPortalService;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Guardian-facing data endpoints. The guardian and school identity always
 * come from the opaque session cookie reconstructed by the security filter.
 */
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor
public class GuardianPortalController {

    private final GuardianPortalService guardianPortalService;

    @GetMapping("/students")
    public List<GuardianStudentResponse> findStudents(
            Authentication authentication
    ) {
        return guardianPortalService.findStudents(
                requirePrincipal(authentication)
        );
    }

    @GetMapping("/access-events")
    public GuardianAccessEventPageResponse findHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) AccessEventType eventType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime occurredFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime occurredTo
    ) {
        return guardianPortalService.findHistory(
                requirePrincipal(authentication),
                page,
                size,
                studentId,
                eventType,
                occurredFrom,
                occurredTo
        );
    }

    @GetMapping("/access-events/{eventId}")
    public GuardianAccessEventResponse findEvent(
            Authentication authentication,
            @PathVariable @Positive Long eventId
    ) {
        return guardianPortalService.findEvent(
                requirePrincipal(authentication),
                eventId
        );
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
