package br.com.infnet.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class UserHeadersFilter extends OncePerRequestFilter {

    //Lista de headers customizados por esse filter
    private static final List<String> CUSTOM_HEADERS = List.of(
            "X-User-Id",
            "X-User-Email",
            "X-User-Nome",
            "X-User-Status",
            "X-User-Allowed",
            "X-Correlation-Id"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Executando UserHeadersFilter, autenticado: {}", auth != null);

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof OAuth2User oauth2User) {
            Map<String, Object> attrs = oauth2User.getAttributes();

            if (!attrs.containsKey("user_id")) {
                log.warn("Atributos customizados não encontrados. O CustomAuthenticationSuccessHandler não foi chamado.");
                chain.doFilter(request, response);
                return;
            }

            //Extrai atributos
            String userId = (String) attrs.get("user_id");
            String email = (String) attrs.get("user_email");
            String nome = (String) attrs.get("user_name");
            String status = (String) attrs.get("user_status");
            boolean allowed = (boolean) attrs.get("user_allowed");
            String correlationId = (String) attrs.get("correlation_id");

            //Se não existir Correlation ID, cria um novo
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Correlation ID gerado: {}", correlationId);
            }

            final String finalUserId = userId;
            final String finalEmail = email;
            final String finalNome = nome;
            final String finalStatus = status;
            final boolean finalAllowed = allowed;
            final String finalCorrelationId = correlationId;

            log.debug("Injetando headers: userId={}, email={}, correlationId={}",
                    finalUserId, finalEmail, finalCorrelationId);

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    return switch (name) {
                        case "X-User-Id" -> finalUserId;
                        case "X-User-Email" -> finalEmail;
                        case "X-User-Nome" -> finalNome;
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
            chain.doFilter(request, response);
        }
    }
}