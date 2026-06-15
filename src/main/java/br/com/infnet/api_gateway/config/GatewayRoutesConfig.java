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

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        log.debug("Registrando rota para USER-SERVICE com path /usuarios/**");
        return route("user-service")
                .route(path("/usuarios/**"), http())
                .before(request -> {
                    log.info("Requisição capturada pela rota: {}", request.uri());
                    return request;
                })
                .filter(lb("USER-SERVICE"))
                .before(request -> {
                    log.info("Headers antes do relay: {}", request.headers());
                    return request;
                })
                .filter(tokenRelay())
                .before(request -> {
                    log.info("Headers depois do relay: {}", request.headers());
                    return request;
                })
                .filter(circuitBreaker("userServiceCB", URI.create("forward:/fallback/user-service")))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> auctionServiceRoute() {
        return route("auction-service")
                .route(path("/api/auctions/**"), http())
                .filter(lb("AUCTION-SERVICE"))
                .filter(tokenRelay())
                .filter(circuitBreaker("auctionServiceCB", URI.create("forward:/fallback/auction-service")))
                .build();
    }
}