package br.com.infnet.api_gateway.auth;

import br.com.infnet.api_gateway.dto.UserStatusResponse;
import br.com.infnet.api_gateway.service.UserStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserStatusService userStatusService;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {

        log.info("Iniciando processamento de login bem-sucedido");
        long startTime = System.currentTimeMillis();

        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();

            // Extrai as informações do token
            assert oauth2User != null;
            Map<String, Object> attrs = new HashMap<>(oauth2User.getAttributes());
            String sub = attrs.get("sub").toString();
            UUID userId = UUID.fromString(sub);
            String email = (String) attrs.get("email");
            String nome = (String) attrs.get("name");

            log.info("Extraindo informações do token: userId={}, email={}, authorities={}",
                    userId, email, oauth2User.getAuthorities());

            // Chama o user-service para obter o status do usuário
            log.info("Consultando status do usuario no user-service: userId={}", userId);
            UserStatusResponse statusResponse = userStatusService.getUserStatus(userId);

            if (statusResponse == null) {
                log.warn("Resposta nula do userStatusService, usando valores padrao para userId={}", userId);
                statusResponse = new UserStatusResponse(userId, "UNKNOWN", false);
            } else {
                log.info("Status do usuario obtido com sucesso: userId={}, status={}, allowed={}",
                        userId, statusResponse.status(), statusResponse.isAllowed());
            }

            // Adiciona os atributos customizados
            attrs.put("user_id", userId.toString());
            attrs.put("user_email", email);
            attrs.put("user_name", nome);
            attrs.put("user_status", statusResponse.status());
            attrs.put("user_allowed", statusResponse.isAllowed());

            log.debug("Atributos enriquecidos: {}", attrs.keySet());

            // Cria um novo OAuth2User com os atributos enriquecidos
            OAuth2User newOAuth2User = new DefaultOAuth2User(oauth2User.getAuthorities(), attrs, "sub");

            // Cria uma nova autenticação
            Authentication newAuth = new OAuth2AuthenticationToken(
                    newOAuth2User,
                    newOAuth2User.getAuthorities(),
                    oauthToken.getAuthorizedClientRegistrationId()
            );

            // Atualiza o SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            log.info("SecurityContext atualizado para usuario: {}", userId);

            // Persiste o SecurityContext na sessão
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            // Redireciona para a página inicial do front-end
            String redirectUrl = "http://localhost:3000/home";
            log.info("Redirecionando para: {}", redirectUrl);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Login concluido com sucesso: userId={}, email={}, duracao={}ms",
                    userId, email, duration);

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Erro durante o processamento do login: {}", e.getMessage(), e);
            throw e;
        }
    }
}