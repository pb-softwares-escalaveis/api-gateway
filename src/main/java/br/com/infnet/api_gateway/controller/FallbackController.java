package br.com.infnet.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {
    @RequestMapping("/fallback/user-service")
    public ResponseEntity<String> usersFallback() {
        return ResponseEntity.status(503).body("Lamentamos, mas o serviço de usuários está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/fallback/auction-service")
    public ResponseEntity<String> auctionFallback() {
        return ResponseEntity.status(503).body("Lamentamos, mas o serviço de lotes está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }
}
