package com.studynexus.controller;

import com.studynexus.model.UserDTO;
import com.studynexus.security.JWTUtil;
import com.studynexus.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final OAuthUserService oAuthUserService;
    private final JWTUtil jwtUtil;

    @GetMapping("/api/user/me")
    public Mono<UserDTO> getCurrentUser(@AuthenticationPrincipal OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            return Mono.empty();
        }

        return oAuthUserService.processOAuthUser(authentication)
                .map(UserDTO::fromUser);
    }

    @GetMapping("/api/auth/token")
    public Mono<Map<String, String>> getToken(@AuthenticationPrincipal OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            return Mono.just(new HashMap<>());
        }

        return oAuthUserService.processOAuthUser(authentication)
                .map(user -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("token", jwtUtil.generateToken(user.getId()));
                    return response;
                });
    }
}