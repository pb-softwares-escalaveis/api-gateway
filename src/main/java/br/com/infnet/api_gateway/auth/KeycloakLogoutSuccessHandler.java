package br.com.infnet.api_gateway.auth;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    @Value("${KEYCLOAK_INTERNAL_URL}")
    private String keycloakInternalUrl;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                Authentication authentication) throws IOException {
        log.info("=== Iniciando logout ===");
        String keycloakLogoutUrl = keycloakInternalUrl + "/realms/leilao-service/protocol/openid-connect/logout";
        StringBuilder logoutUrl = new StringBuilder(keycloakLogoutUrl);

        //Tenta obter o ID token para revogar a sessão no Keycloak
        String idToken = null;

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            //Para OIDC
            if (principal instanceof OidcUser oidcUser) {
                idToken = oidcUser.getIdToken().getTokenValue();
                log.debug("ID Token obtido do OidcUser");
            }
            //Para OAuth2
            else if (principal instanceof OAuth2User) {
                //Tenta obter da sessão
                Object idTokenAttr = request.getSession().getAttribute("id_token");
                if (idTokenAttr != null) {
                    idToken = idTokenAttr.toString();
                    log.debug("ID Token obtido da sessão");
                }
            }
        }

        //Adiciona parâmetros à URL
        boolean firstParam = true;

        if (idToken != null) {
            logoutUrl.append("?id_token_hint=").append(idToken);
            firstParam = false;
        }

        String encodedRedirectUri = URLEncoder.encode(frontendUrl, StandardCharsets.UTF_8);
        //Adiciona redirect após logout
        if (firstParam) {
            logoutUrl.append("?");
        } else {
            logoutUrl.append("&");
        }
        logoutUrl.append("post_logout_redirect_uri=").append(encodedRedirectUri);

        log.info("Redirecionando para: {}", logoutUrl);

        //Redireciona para o logout do Keycloak
        response.sendRedirect(logoutUrl.toString());
    }
}