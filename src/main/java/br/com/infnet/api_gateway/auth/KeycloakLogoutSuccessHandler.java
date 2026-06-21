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

    @Value("${KEYCLOAK_EXTERNAL_URL}")
    private String keycloakExternalUrl;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                Authentication authentication) throws IOException {

        log.info("Iniciando processo de logout");
        long startTime = System.currentTimeMillis();

        String userId = null;
        String email;

        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            userId = oauth2User.getAttribute("user_id");
            email = oauth2User.getAttribute("email");
            if (userId == null) {
                userId = oauth2User.getAttribute("sub");
            }
            log.info("Usuário autenticado durante logout: userId={}, email={}", userId, email);
        } else {
            log.debug("Nenhum usuário autenticado encontrado durante logout");
        }

        try {
            String keycloakLogoutUrl = keycloakExternalUrl + "/realms/leilao-service/protocol/openid-connect/logout";
            StringBuilder logoutUrl = new StringBuilder(keycloakLogoutUrl);

            // Tenta obter o ID token para revogar a sessão no Keycloak
            String idToken = null;

            if (authentication != null) {
                Object principal = authentication.getPrincipal();

                // Para OIDC
                if (principal instanceof OidcUser oidcUser) {
                    idToken = oidcUser.getIdToken().getTokenValue();
                    log.info("ID Token obtido do OidcUser para usuario: {}", userId);
                }
                // Para OAuth2
                else if (principal instanceof OAuth2User) {
                    Object idTokenAttr = request.getSession().getAttribute("id_token");
                    if (idTokenAttr != null) {
                        idToken = idTokenAttr.toString();
                        log.info("ID Token obtido da sessão para usuário: {}", userId);
                    } else {
                        log.debug("ID Token não encontrado na sessão");
                    }
                }
            }

            boolean firstParam = true;

            if (idToken != null && !idToken.isEmpty()) {
                logoutUrl.append("?id_token_hint=").append(idToken);
                firstParam = false;
                log.debug("ID Token adicionado a URL de logout");
            } else {
                log.warn("ID Token nao disponível, logout será realizado sem revogação do token");
            }

            String encodedRedirectUri = URLEncoder.encode(frontendUrl, StandardCharsets.UTF_8);
            log.debug("Redirect URI codificada: {}", encodedRedirectUri);

            if (firstParam) {
                logoutUrl.append("?");
            } else {
                logoutUrl.append("&");
            }
            logoutUrl.append("post_logout_redirect_uri=").append(encodedRedirectUri);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Redirecionando para logout do Keycloak: url={}, usuário={}, duração={}ms",
                    logoutUrl, userId, duration);

            // Redireciona para o logout do Keycloak
            response.sendRedirect(logoutUrl.toString());

            log.info("Logout concluído com sucesso para usuario: {}", userId);

        } catch (Exception e) {
            log.error("Erro durante o processo de logout para usuário {}: {}", userId, e.getMessage(), e);
            // Mesmo com erro, tenta redirecionar para o frontend
            response.sendRedirect(frontendUrl);
        }
    }
}