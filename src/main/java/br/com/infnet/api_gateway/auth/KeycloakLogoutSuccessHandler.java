package br.com.infnet.api_gateway.auth;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {

    @Value("${KEYCLOAK_EXTERNAL_URL}")
    private String keycloakExternalUrl;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:api-gateway}")
    private String clientId;

    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                Authentication authentication) throws IOException {

        log.info("Iniciando processo de logout");

        String keycloakLogoutUrl = keycloakExternalUrl + "/realms/leilao-service/protocol/openid-connect/logout";
        StringBuilder logoutUrl = new StringBuilder(keycloakLogoutUrl);

        log.warn("Realizando logout para client_id={} com post_logout_redirect_uri={}", clientId, frontendUrl);
        logoutUrl.append("?client_id=").append(clientId);

        String encodedRedirectUri = URLEncoder.encode(frontendUrl, StandardCharsets.UTF_8);
        logoutUrl.append("&post_logout_redirect_uri=").append(encodedRedirectUri);

        log.info("Redirecionando para logout: {}", logoutUrl);
        response.sendRedirect(logoutUrl.toString());
    }
}