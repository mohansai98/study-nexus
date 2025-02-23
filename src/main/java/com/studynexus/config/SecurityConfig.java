package com.studynexus.config;

import com.studynexus.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuthUserService oAuthUserService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/login", "/oauth2/**", "/webjars/**", "/css/**", "/js/**").permitAll()
                        .pathMatchers("/api/public/**").permitAll()
                        .pathMatchers("/api/chat/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                )
                .build();
    }

    @Bean
    public ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        return (webFilterExchange, authentication) -> {
            // After successful authentication, redirect to the dashboard
            ServerWebExchange exchange = webFilterExchange.getExchange();
            exchange.getResponse().getHeaders().setLocation(URI.create("/dashboard"));
            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
            return Mono.empty();
        };
    }

    @Bean
    public ServerLogoutSuccessHandler logoutSuccessHandler() {
        return (webFilterExchange, authentication) -> {
            // After logout, redirect to the home page
            ServerWebExchange exchange = webFilterExchange.getExchange();
            exchange.getResponse().getHeaders().setLocation(URI.create("/"));
            exchange.getResponse().setStatusCode(HttpStatus.FOUND);
            return Mono.empty();
        };
    }
}
