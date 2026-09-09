package com.graduacionesisamar.controlescolar.security.config;

import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianSessionAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GuardianSessionAuthenticationFilter guardianSessionFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.spa())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/login",
                                "/api/v1/guardian-device-enrollments/complete",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/v1/schools/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/student-imports/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/guardian-imports/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/guardian-activations/**").hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/guardians/*/device-enrollments"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/v1/guardian/**").hasRole("GUARDIAN")
                        .anyRequest().authenticated()
                )
                .addFilterAfter(
                        guardianSessionFilter,
                        SecurityContextHolderFilter.class
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(
                                        HttpServletResponse.SC_NO_CONTENT
                                )
                        )
                        .failureHandler((request, response, exception) ->
                                writeJsonError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Correo o contraseña incorrectos"
                                )
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(
                                        HttpServletResponse.SC_NO_CONTENT
                                )
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        writeJsonError(
                                                response,
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Se requiere iniciar sesión"
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        writeJsonError(
                                                response,
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Acceso denegado"
                                        )
                        )
                );

        return http.build();
    }

    private static void writeJsonError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"message\":\"" + message + "\"}"
        );
    }
}
