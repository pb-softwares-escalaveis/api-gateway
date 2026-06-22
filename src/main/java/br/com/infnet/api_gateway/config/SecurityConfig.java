package br.com.infnet.api_gateway.config;

import br.com.infnet.api_gateway.auth.CustomAuthenticationSuccessHandler;
import br.com.infnet.api_gateway.auth.KeycloakLogoutSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    // Injeção via construtor (recomendado)
    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    // ============================================================
    // FILTRO PÚBLICO
    // ============================================================
    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) {
        http
                .securityMatcher(
                        // Gateway
                        "/health",
                        "/actuator/health",
                        "/actuator/circuitbreakers",
                        "/fallback/**",

                        // User-service públicos
                        "/usuarios/novo",
                        "/usuarios/listar-usernames",
                        "/usuarios/{id}/perfil",
                        "/usuarios/{id}/seller-info",

                        // Outros serviços públicos
                        "/auctions/{auctionId}",
                        "/api/qa/auctions/{auctionId}/questions",
                        "/listings/**",
                        "/recommendations/**"
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2ResourceServer(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // ============================================================
    // FILTRO PRIVADO
    // ============================================================
    @Bean
    @Order(2)
    public SecurityFilterChain privateFilterChain(HttpSecurity http,
                                                  CustomAuthenticationSuccessHandler successHandler,
                                                  KeycloakLogoutSuccessHandler logoutSuccessHandler) {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth
                        // Rotas privadas
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/deletar/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions/{auctionId}/renew").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions/{auctionId}/bids/place").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/auctions/{auctionId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/qa/auctions/{auctionId}/questions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/qa/questions/{questionId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/qa/questions/{questionId}/answers").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/qa/questions/{questionId}/answers/{answerId}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/listar-pfps").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/trocar-pfp").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions/create").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auctions").authenticated()
                        .requestMatchers(HttpMethod.GET, "/payments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/simulate/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/transactions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/transactions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/report-auction/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/report-message/**").authenticated()

                        // Rotas internas bloqueadas
                        .requestMatchers(HttpMethod.GET, "/usuarios/status").denyAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/{id}").denyAll()
                        // Fallback
                        .anyRequest().authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
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
                .csrf(AbstractHttpConfigurer::disable)
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