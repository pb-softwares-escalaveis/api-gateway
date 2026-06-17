package br.com.infnet.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user-service")
    public ResponseEntity<String> userServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de usuários está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/auction-service")
    public ResponseEntity<String> auctionServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de lotes está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/listing-service")
    public ResponseEntity<String> listingServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de listagem de anúncios está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/payment-service")
    public ResponseEntity<String> paymentServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de pagamentos está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/transaction-service")
    public ResponseEntity<String> transactionServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de transações está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/report-service")
    public ResponseEntity<String> reportServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de reports está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }

    @RequestMapping("/qa-service")
    public ResponseEntity<String> qaServiceFallback() {
        return ResponseEntity.status(503)
                .body("Lamentamos, mas o serviço de perguntas e respostas está temporariamente indisponível. Por favor, tente novamente mais tarde.");
    }
}