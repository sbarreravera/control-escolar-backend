package com.graduacionesisamar.controlescolar.qrtest.controller;

import com.graduacionesisamar.controlescolar.qrtest.dto.QrTestRequest;
import com.graduacionesisamar.controlescolar.qrtest.dto.QrTestResponse;
import com.graduacionesisamar.controlescolar.qrtest.service.QrTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pruebas de QR de solo lectura para administradores.
 * Nunca reutilizar AccessEventService.scan() desde este controlador.
 */
@RestController
@RequestMapping("/api/v1/qr-tests")
@RequiredArgsConstructor
public class QrTestController {
    private final QrTestService qrTestService;

    @PostMapping("/validate")
    public QrTestResponse validate(@Valid @RequestBody QrTestRequest request) {
        return qrTestService.validate(request);
    }
}
