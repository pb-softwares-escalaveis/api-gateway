package br.com.infnet.api_gateway.service;

import br.com.infnet.api_gateway.dto.UserStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatusService {

    private final RestClient.Builder restClientBuilder;

    public UserStatusResponse getUserStatus(UUID userId) {
        try {
            RestClient restClient = restClientBuilder.build();
            List<UserStatusResponse> response = restClient.get()
                    .uri("lb://USER-SERVICE/usuarios/status?ids={userId}", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserStatusResponse>>() {});

            if (response != null && !response.isEmpty()) {
                return response.getFirst();
            }
            return new UserStatusResponse(userId, "INATIVO", false);
        } catch (Exception e) {
            log.error("Falha ao obter status do usuário {}: {}", userId, e.getMessage());
            return new UserStatusResponse(userId, "INATIVO", false);
        }
    }
}