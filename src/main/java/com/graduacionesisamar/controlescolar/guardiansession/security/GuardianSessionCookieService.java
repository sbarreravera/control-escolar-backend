package com.graduacionesisamar.controlescolar.guardiansession.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * Owns the guardian session cookie contract.
 */
@Component
public class GuardianSessionCookieService {

    public static final String COOKIE_NAME = "GUARDIAN_SESSION";
    private static final String COOKIE_PATH = "/api/v1/guardian";

    private final boolean secureCookie;

    public GuardianSessionCookieService(
            @Value("${app.guardian-session.secure-cookie:true}")
            boolean secureCookie
    ) {
        this.secureCookie = secureCookie;
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public void write(
            HttpServletResponse response,
            String token,
            OffsetDateTime expiresAt
    ) {
        Duration maxAge = Duration.between(
                OffsetDateTime.now(),
                expiresAt
        );

        ResponseCookie cookie = baseCookie(token)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(
            String value
    ) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }
}
