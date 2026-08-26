package com.example.application.login_module.security;

import com.example.application.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        // Injects Spring Boot's auto-configured ObjectMapper bean (which registers all available
        // Jackson modules, including jackson-datatype-jsr310 for java.time types like Instant)
        // instead of a bare `new ObjectMapper()`, which has no modules registered at all and
        // throws InvalidDefinitionException the moment it tries to serialize ErrorResponse's
        // Instant timestamp field - exactly what was happening on every 401 response.
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse("Authentication is required to access this resource",
                HttpStatus.UNAUTHORIZED.value(), request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
