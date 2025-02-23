package com.studynexus.controller;

import com.studynexus.dto.UserDTO;
import com.studynexus.security.JWTUtil;
import com.studynexus.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
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
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        }
        return oAuthUserService.processOAuthUser(authentication)
                .map(UserDTO::fromUser);
    }

    @GetMapping("/api/auth/token")
    public Mono<Map<String, String>> getToken(@AuthenticationPrincipal OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        }

        String email = authentication.getPrincipal().getAttribute("email");
        if (email == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth provider did not return an email"));
        }
        return oAuthUserService.findUserByEmail(email)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found. Please complete registration first.")))
                .map(user -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("token", jwtUtil.generateToken(user.getId()));
                    return response;
                });
    }

    @GetMapping("/api/users/list")
    public Flux<UserDTO> getAllUsers(@AuthenticationPrincipal OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            return Flux.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        }
        return oAuthUserService.getAllUsers().map(UserDTO::fromUser);
    }
}