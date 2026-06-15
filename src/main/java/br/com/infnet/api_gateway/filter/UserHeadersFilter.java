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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        log.debug("Executando UserHeadersFilter, autenticado: {}",
                SecurityContextHolder.getContext().getAuthentication() != null);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof OAuth2User oauth2User) {
            Map<String, Object> attrs = oauth2User.getAttributes();

            if (!attrs.containsKey("user_id")) {
                log.warn("Atributos customizados não encontrados. O CustomOAuth2UserService não foi chamado.");
                chain.doFilter(request, response);
                return;
            }

            String userId = (String) attrs.get("user_id");
            String email = (String) attrs.get("user_email");
            String nome = (String) attrs.get("user_name");
            String status = (String) attrs.get("user_status");
            boolean allowed = (boolean) attrs.get("user_allowed");

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    return switch (name) {
                        case "X-User-Id" -> userId;
                        case "X-User-Email" -> email;
                        case "X-User-Nome" -> nome;
                        case "X-User-Status" -> status;
                        case "X-User-Allowed" -> String.valueOf(allowed);
                        default -> super.getHeader(name);
                    };
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> names = new HashSet<>();
                    Enumeration<String> original = super.getHeaderNames();
                    while (original.hasMoreElements()) names.add(original.nextElement());
                    names.addAll(List.of("X-User-Id", "X-User-Email", "X-User-Nome", "X-User-Status", "X-User-Allowed"));
                    return Collections.enumeration(names);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (name.startsWith("X-User-")) {
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
