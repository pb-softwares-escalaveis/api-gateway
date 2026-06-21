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
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        //Extrai as informações do token
        assert oauth2User != null;
        Map<String, Object> attrs = new HashMap<>(oauth2User.getAttributes());
        String sub = attrs.get("sub").toString();
        UUID userId = UUID.fromString(sub);
        String email = (String) attrs.get("email");
        String nome = (String) attrs.get("name");

        log.info("Enriquecendo usuário após login: userId={}, email={}", userId, email);

        //Chama o user-service para obter o status do usuário
        UserStatusResponse statusResponse = userStatusService.getUserStatus(userId);
        if (statusResponse == null) {
            log.warn("Resposta nula do userStatusService, usando valores padrão");
            statusResponse = new UserStatusResponse(userId, "UNKNOWN", false);
        }

        //Adiciona os atributos customizados
        attrs.put("user_id", userId.toString());
        attrs.put("user_email", email);
        attrs.put("user_name", nome);
        attrs.put("user_status", statusResponse.status());
        attrs.put("user_allowed", statusResponse.isAllowed());

        //Cria um novo OAuth2User com os atributos enriquecidos
        OAuth2User newOAuth2User = new DefaultOAuth2User(oauth2User.getAuthorities(), attrs, "sub");

        //Cria uma nova autenticação
        Authentication newAuth = new OAuth2AuthenticationToken(
                newOAuth2User,
                newOAuth2User.getAuthorities(),
                oauthToken.getAuthorizedClientRegistrationId()
        );

        //Atualiza o SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        //Persiste o SecurityContext na sessão
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        //Redireciona para a página inicial do front-end
        response.sendRedirect("http://localhost:3000/home");
    }
}