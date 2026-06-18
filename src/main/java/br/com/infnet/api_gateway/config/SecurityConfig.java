package br.com.infnet.api_gateway.config;

import br.com.infnet.api_gateway.auth.CustomAuthenticationSuccessHandler;
import br.com.infnet.api_gateway.auth.KeycloakLogoutSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomAuthenticationSuccessHandler successHandler,
                                           KeycloakLogoutSuccessHandler logoutSuccessHandler) {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // ============================================================
                        // 1. ENDPOINTS INTERNOS (somente para comunicação entre serviços)
                        // ============================================================
                        .requestMatchers(
                                "/usuarios/status",
                                "/usuarios/{id}"
                        ).denyAll()

                        // ============================================================
                        // 2. ENDPOINTS DO GATEWAY
                        // ============================================================
                        .requestMatchers(
                                "/health",
                                "/actuator/health",
                                "/actuator/circuitbreakers",
                                "/fallback/**",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // ============================================================
                        // 3. USER-SERVICE PÚBLICOS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/usuarios/{id}/perfil").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/{id}/seller-info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/listar-usernames").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios/novo").permitAll()

                        // ============================================================
                        // 4. AUCTION-SERVICE PÚBLICOS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/auctions/{auctionId}").permitAll()

                        // ============================================================
                        // 5. LISTING-SERVICE (TUDO PÚBLICO)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/listings/**").permitAll()

                        // ============================================================
                        // 6. RECOMMENDATION-SERVICE (TUDO PÚBLICO)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/recommendations/**").permitAll()

                        // ============================================================
                        // 7. QA-SERVICE PÚBLICOS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/qa/auctions/{auctionId}/questions").permitAll()

                        // ============================================================
                        // 8. USER-SERVICE PRIVADOS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/usuarios/listar-pfps").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/trocar-pfp").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/{id}").authenticated()

                        // ============================================================
                        // 9. AUCTION-SERVICE PRIVADOS
                        // ============================================================
                        .requestMatchers(HttpMethod.POST, "/auctions/create").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions/{auctionId}/renew").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions/{auctionId}/bids/place").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/auctions/{auctionId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions").authenticated()

                        // ============================================================
                        // 10. QA-SERVICE PRIVADOS
                        // ============================================================
                        .requestMatchers(HttpMethod.POST, "/api/qa/auctions/{auctionId}/questions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/qa/questions/{questionId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/qa/questions/{questionId}/answers").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/qa/questions/{questionId}/answers/{answerId}").authenticated()

                        // ============================================================
                        // 11. PAYMENT-SERVICE (TUDO PRIVADO)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/payments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/simulate/**").authenticated()

                        // ============================================================
                        // 12. REPORT-SERVICE (TUDO PRIVADO)
                        // ============================================================
                        .requestMatchers(HttpMethod.POST, "/report-auction/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/report-message/**").authenticated()

                        // ============================================================
                        // 13. TRANSACTION-SERVICE (TUDO PRIVADO)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/transactions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/transactions/**").authenticated()

                        // ============================================================
                        // 14. FALLBACK
                        // ============================================================
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                )
                .oauth2Client(oauth2 -> oauth2
                        .authorizationCodeGrant(codeGrant -> codeGrant
                                .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository())
                        )
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }
}