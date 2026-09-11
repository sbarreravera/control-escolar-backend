package com.graduacionesisamar.controlescolar.communication.controller;

import com.graduacionesisamar.controlescolar.communication.dto.*;
import com.graduacionesisamar.controlescolar.communication.service.SchoolCommunicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/communications")
@RequiredArgsConstructor
@Validated
public class SchoolCommunicationController {

    private final SchoolCommunicationService communicationService;

    @PostMapping("/preview")
    public CommunicationAudiencePreviewResponse preview(
            @Valid @RequestBody CommunicationAudienceRequest request
    ) {
        return communicationService.preview(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse create(
            @Valid @RequestBody CreateCommunicationRequest request
    ) {
        return communicationService.create(request);
    }

    @PutMapping("/{communicationId}")
    public CommunicationResponse updateScheduled(
            @PathVariable @Positive Long communicationId,
            @Valid @RequestBody CreateCommunicationRequest request
    ) {
        return communicationService.updateScheduled(communicationId, request);
    }

    @PostMapping("/{communicationId}/cancel")
    public CommunicationResponse cancel(
            @PathVariable @Positive Long communicationId
    ) {
        return communicationService.cancel(communicationId);
    }

    @GetMapping
    public List<CommunicationResponse> findHistory(
            @RequestParam @Positive Long schoolId
    ) {
        return communicationService.findHistory(schoolId);
    }

    @GetMapping("/{communicationId}")
    public CommunicationResponse findOne(
            @PathVariable @Positive Long communicationId
    ) {
        return communicationService.findOne(communicationId);
    }

    @GetMapping("/{communicationId}/recipients")
    public List<CommunicationRecipientResponse> findRecipients(
            @PathVariable @Positive Long communicationId
    ) {
        return communicationService.findRecipients(communicationId);
    }
}
