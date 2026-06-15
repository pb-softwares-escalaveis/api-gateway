package br.com.infnet.api_gateway.config;

import br.com.infnet.api_gateway.auth.CustomAuthenticationSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomAuthenticationSuccessHandler successHandler) {
        log.debug("=== Configurando SecurityFilterChain com success handler ===");
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health", "/actuator/health", "/fallback/**", "/test/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                );
        return http.build();
    }
}