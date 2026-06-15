package br.com.infnet.api_gateway.service;

import br.com.infnet.api_gateway.dto.UserStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserStatusService {

    private final RestClient restClient;

    public UserStatusService(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public UserStatusResponse getUserStatus(UUID userId) {
        log.info("Chamando user-service para status do usuário: {}", userId);
        try {
            List<UserStatusResponse> responses = restClient.get()
                    .uri("lb://USER-SERVICE/usuarios/status?ids={userId}", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (responses != null && !responses.isEmpty()) {
                UserStatusResponse response = responses.getFirst();
                log.info("Status recebido: userId={}, status={}, allowed={}",
                        response.userId(), response.status(), response.isAllowed());
                return response;
            } else {
                log.warn("Resposta vazia para userId: {}", userId);
                return new UserStatusResponse(userId, "UNKNOWN", false);
            }
        } catch (Exception e) {
            log.error("Erro ao chamar user-service para userId: {}", userId, e);
            return new UserStatusResponse(userId, "UNKNOWN", false);
        }
    }
}