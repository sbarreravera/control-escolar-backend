package com.graduacionesisamar.controlescolar.accessevent.controller;

import com.graduacionesisamar.controlescolar.accessevent.dto.AccessEventResponse;
import com.graduacionesisamar.controlescolar.accessevent.dto.ScanAccessEventRequest;
import com.graduacionesisamar.controlescolar.accessevent.service.AccessEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes REST operations for student access events.
 */
@RestController
@RequestMapping("/api/v1/access-events")
@RequiredArgsConstructor
public class AccessEventController {

    private final AccessEventService accessEventService;

    /**
     * Registers an entry or exit from an active QR credential.
     */
    @PostMapping("/scan")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessEventResponse scan(
            @Valid @RequestBody ScanAccessEventRequest request
    ) {
        return accessEventService.scan(request);
    }
}