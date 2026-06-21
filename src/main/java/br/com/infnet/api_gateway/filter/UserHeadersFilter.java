package br.com.infnet.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHeadersFilter extends OncePerRequestFilter {

    private static final List<String> CUSTOM_HEADERS = List.of(
            "X-User-Id",
            "X-User-Email",
            "X-User-Name",
            "X-User-Status",
            "X-User-Allowed",
            "X-Correlation-Id"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Executando UserHeadersFilter, autenticado: {}", auth != null);

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof OAuth2User oauth2User) {

            Map<String, Object> attrs = oauth2User.getAttributes();

            if (!attrs.containsKey("user_id")) {
                log.warn("Atributos customizados não encontrados.");
                chain.doFilter(request, response);
                return;
            }

            String userId = (String) attrs.get("user_id");
            String email = (String) attrs.get("user_email");
            String name = (String) attrs.get("user_name");
            String status = (String) attrs.get("user_status");
            Boolean allowed = (Boolean) attrs.get("user_allowed");

            final String finalUserId = userId;
            final String finalEmail = email;
            final String finalName = name;
            final String finalStatus = status;
            final boolean finalAllowed = allowed != null && allowed;
            final String finalCorrelationId = correlationId;

            log.debug("Injetando headers: userId={}, email={}, correlationId={}",
                    finalUserId, finalEmail, finalCorrelationId);

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    return switch (name) {
                        case "X-User-Id" -> finalUserId;
                        case "X-User-Email" -> finalEmail;
                        case "X-User-Name" -> finalName;
                        case "X-User-Status" -> finalStatus;
                        case "X-User-Allowed" -> String.valueOf(finalAllowed);
                        case "X-Correlation-Id" -> finalCorrelationId;
                        default -> super.getHeader(name);
                    };
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> names = new HashSet<>();
                    Enumeration<String> original = super.getHeaderNames();
                    while (original.hasMoreElements()) {
                        names.add(original.nextElement());
                    }
                    names.addAll(CUSTOM_HEADERS);
                    return Collections.enumeration(names);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (CUSTOM_HEADERS.contains(name)) {
                        return Collections.enumeration(List.of(getHeader(name)));
                    }
                    return super.getHeaders(name);
                }
            };

            chain.doFilter(wrappedRequest, response);

        } else {
            log.debug("Usuário não autenticado, apenas correlationId será adicionado");
            HttpServletRequest wrappedRequest = getHttpServletRequest(request, correlationId);
            chain.doFilter(wrappedRequest, response);
        }
    }

    private static @NonNull HttpServletRequest getHttpServletRequest(@NonNull HttpServletRequest request, String correlationId) {
        final String finalCorrelationId = correlationId;

        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-Correlation-Id".equals(name)) {
                    return finalCorrelationId;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> names = new HashSet<>();
                Enumeration<String> original = super.getHeaderNames();
                while (original.hasMoreElements()) {
                    names.add(original.nextElement());
                }
                names.add("X-Correlation-Id");
                return Collections.enumeration(names);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-Correlation-Id".equals(name)) {
                    return Collections.enumeration(List.of(finalCorrelationId));
                }
                return super.getHeaders(name);
            }
        };
    }
}