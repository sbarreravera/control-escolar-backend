package com.graduacionesisamar.controlescolar.auth.dto;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token
) {
}