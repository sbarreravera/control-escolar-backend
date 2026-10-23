package com.graduacionesisamar.controlescolar.qrtest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Contexto obligatorio para evitar consultar códigos de otra escuela.
 */
public record QrTestRequest(
        @NotBlank @Size(max = 100) String qrToken,
        @NotNull @Positive Long schoolId
) {}
