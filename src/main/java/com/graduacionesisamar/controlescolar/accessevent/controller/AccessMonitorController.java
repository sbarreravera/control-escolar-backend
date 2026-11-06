package com.graduacionesisamar.controlescolar.accessevent.controller;

import com.graduacionesisamar.controlescolar.accessevent.dto.AccessMonitorFilterOptions;
import com.graduacionesisamar.controlescolar.accessevent.dto.AccessMonitorPageResponse;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.service.AccessMonitorService;
import com.graduacionesisamar.controlescolar.security.module.SchoolModule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Lists already-recorded access events. Never scans or queues notifications.
 */
@RestController
@SchoolModule(
        key = "ACCESS_MONITOR",
        name = "Monitor de accesos",
        description = "Consultar accesos diarios de alumnos con filtros y paginación.",
        defaultGranted = false,
        order = 16
)
@RequestMapping("/api/v1/access-events")
@RequiredArgsConstructor
@Validated
public class AccessMonitorController {

    private final AccessMonitorService accessMonitorService;

    @GetMapping("/monitor")
    public AccessMonitorPageResponse findPage(
            @RequestParam @Positive Long schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 150) String search,
            @RequestParam(required = false) @Positive Long academicCycleId,
            @RequestParam(required = false) @Positive Long schoolGroupId,
            @RequestParam(required = false) AccessEventType eventType
    ) {
        return accessMonitorService.findPage(
                schoolId,
                date,
                page,
                size,
                search,
                academicCycleId,
                schoolGroupId,
                eventType
        );
    }
}
