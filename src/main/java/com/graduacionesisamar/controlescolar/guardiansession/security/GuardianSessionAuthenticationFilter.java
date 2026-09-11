package com.graduacionesisamar.controlescolar.guardiansession.security;

import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates portal requests from the opaque HttpOnly cookie.
 */
@Component
@RequiredArgsConstructor
public class GuardianSessionAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String GUARDIAN_API_PREFIX =
            "/api/v1/guardian/";

    private final GuardianSessionCookieService cookieService;
    private final GuardianSessionService guardianSessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(
                GUARDIAN_API_PREFIX
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        cookieService.read(request)
                .flatMap(guardianSessionService::authenticate)
                .ifPresent(principal -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_GUARDIAN"
                                    ))
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }
}
