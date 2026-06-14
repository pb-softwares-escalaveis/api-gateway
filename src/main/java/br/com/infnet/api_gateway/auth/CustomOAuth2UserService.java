package br.com.infnet.api_gateway.auth;

import br.com.infnet.api_gateway.dto.UserStatusResponse;
import br.com.infnet.api_gateway.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserStatusService userStatusService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        //Carrega dados básicos do Keycloak
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        String sub = oauth2User.getAttribute("sub");
        assert sub != null;
        UUID userId = UUID.fromString(sub);
        String email = oauth2User.getAttribute("email");
        String nome = oauth2User.getAttribute("name");

        UserStatusResponse statusResponse = userStatusService.getUserStatus(userId);

        Map<String, Object> attrs = new HashMap<>(oauth2User.getAttributes());
        attrs.put("user_id", userId.toString());
        attrs.put("user_email", email);
        attrs.put("user_name", nome);
        attrs.put("user_status", statusResponse.status());
        attrs.put("user_allowed", statusResponse.isAllowed());

        return new DefaultOAuth2User(oauth2User.getAuthorities(), attrs, userNameAttributeName);
    }
}
