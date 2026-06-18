package br.com.infnet.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions.circuitBreaker;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Slf4j
@Configuration
public class GatewayRoutesConfig {

    public GatewayRoutesConfig() {
        log.debug("GatewayRoutesConfig carregada!");
    }

    // ============================================================
    // USER-SERVICE
    // ============================================================

    //Rotas públicas
    @Bean
    public RouterFunction<ServerResponse> userServicePublicRoutes() {
        return route("user-service-public")
                .route(path("/usuarios/{id}/perfil"), http())
                .route(path("/usuarios/{id}/seller-info"), http())
                .route(path("/usuarios/listar-usernames"), http())
                .filter(lb("USER-SERVICE"))
                .build();
    }

    //Rota de criação
    @Bean
    public RouterFunction<ServerResponse> userServiceCreateRoute() {
        return route("user-service-create")
                .route(path("/usuarios/novo"), http())
                .filter(lb("USER-SERVICE"))
                .filter(circuitBreaker("userServiceCB", URI.create("forward:/fallback/user-service")))
                .build();
    }

    //Rotas autenticadas
    @Bean
    public RouterFunction<ServerResponse> userServiceProtectedRoutes() {
        return route("user-service-protected")
                .route(path("/usuarios/listar-pfps"), http())
                .route(path("/usuarios/trocar-pfp"), http())
                .route(path("/usuarios/{id}"), http())
                .filter(lb("USER-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("userServiceCB", URI.create("forward:/fallback/user-service")))
                .build();
    }

    // ============================================================
    // AUCTION-SERVICE
    // ============================================================

    //Rota pública
    @Bean
    public RouterFunction<ServerResponse> auctionServicePublicRoutes() {
        return route("auction-service-public")
                .route(path("/auctions/{auctionId}"), http())
                .filter(lb("AUCTION-SERVICE"))
                .build();
    }

    //Rotas autenticadas
    @Bean
    public RouterFunction<ServerResponse> auctionServiceProtectedRoutes() {
        return route("auction-service-protected")
                .route(path("/auctions/create"), http())
                .route(path("/auctions/{auctionId}/renew"), http())
                .route(path("/auctions/{auctionId}/bids/place"), http())
                .route(path("/auctions/{auctionId}"), http())
                .route(path("/auctions"), http())
                .filter(lb("AUCTION-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("auctionServiceCB", URI.create("forward:/fallback/auction-service")))
                .build();
    }

    // ============================================================
    // LISTING-SERVICE (Tudo Público)
    // ============================================================

    @Bean
    public RouterFunction<ServerResponse> listingServiceRoutes() {
        return route("listing-service")
                .route(path("/listings/**"), http())
                .filter(lb("LISTING-SERVICE"))
                .build();
    }

    // ============================================================
    // RECOMMENDATION-SERVICE (Tudo Público)
    // ============================================================

    @Bean
    public RouterFunction<ServerResponse> recommendationServiceRoutes() {
        return route("recommendation-service")
                .route(path("/recommendations/**"), http())
                .filter(lb("RECOMMENDATION-SERVICE"))
                .build();
    }

    // ============================================================
    // PAYMENT-SERVICE (Tudo Privado)
    // ============================================================

    @Bean
    public RouterFunction<ServerResponse> paymentServiceProtectedRoutes() {
        return route("payment-service-protected")
                .route(path("/payments/{id}"), http())
                .route(path("/payments/transaction/{id}"), http())
                .route(path("/payments/auction/{id}"), http())
                .route(path("/payments/bidder/{id}"), http())
                .route(path("/simulate/{providerPaymentId}"), http())
                .filter(lb("PAYMENT-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("paymentServiceCB", URI.create("forward:/fallback/payment-service")))
                .build();
    }

    // ============================================================
    // QA-SERVICE (Perguntas e Respostas)
    // ============================================================

    // Rota pública
    @Bean
    public RouterFunction<ServerResponse> qaServicePublicRoutes() {
        return route("qa-service-public")
                .route(path("/api/qa/auctions/{auctionId}/questions"), http())
                .filter(lb("QA-SERVICE"))
                .build();
    }

    // Rotas privadas
    @Bean
    public RouterFunction<ServerResponse> qaServiceProtectedRoutes() {
        return route("qa-service-protected")
                .route(path("/api/qa/auctions/{auctionId}/questions"), http())
                .route(path("/api/qa/questions/{questionId}"), http())
                .route(path("/api/qa/questions/{questionId}/answers"), http())
                .route(path("/api/qa/questions/{questionId}/answers/{answerId}"), http())
                .filter(lb("QA-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("qaServiceCB", URI.create("forward:/fallback/qa-service")))
                .build();
    }

    // ============================================================
    // TRANSACTION-SERVICE (Tudo Privado)
    // ============================================================

    @Bean
    public RouterFunction<ServerResponse> transactionServiceProtectedRoutes() {
        return route("transaction-service-protected")
                .route(path("/transactions/{id}"), http())
                .route(path("/transactions/{id}/confirm-delivery"), http())
                .filter(lb("TRANSACTION-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("transactionServiceCB", URI.create("forward:/fallback/transaction-service")))
                .build();
    }

    // ============================================================
    // REPORT-SERVICE (Tudo Privado)
    // ============================================================

    @Bean
    public RouterFunction<ServerResponse> reportServiceProtectedRoutes() {
        return route("report-service-protected")
                .route(path("/report-auction/**"), http())
                .route(path("/report-message/**"), http())
                .filter(lb("REPORT-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("reportServiceCB", URI.create("forward:/fallback/report-service")))
                .build();
    }

}