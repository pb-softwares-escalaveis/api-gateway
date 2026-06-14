package br.com.infnet.api_gateway.dto;

import java.util.UUID;

public record UserStatusResponse(
        UUID userId,
        String status,
        boolean isAllowed
) {}
