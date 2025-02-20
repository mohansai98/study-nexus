package com.studynexus.service;

import com.studynexus.model.User;
import com.studynexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthUserService {

    private final UserRepository userRepository;

    public Mono<User> processOAuthUser(OAuth2AuthenticationToken authentication) {
        OAuth2User oAuth2User = authentication.getPrincipal();
        String provider = authentication.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = attributes.get("email").toString();
        String providerId = attributes.get("sub").toString();

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .flatMap(user -> {
                    // User found, update last login
                    user.setLastLogin(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .switchIfEmpty(
                        // If not found by provider, try by email
                        userRepository.findByEmail(email)
                                .flatMap(existingUser -> {
                                    // Found by email, update provider details
                                    existingUser.setProvider(provider);
                                    existingUser.setProviderId(providerId);
                                    existingUser.setLastLogin(LocalDateTime.now());
                                    return userRepository.save(existingUser);
                                })
                                .switchIfEmpty(
                                        // Not found by either method, create new
                                        createNewUser(attributes, provider, providerId)
                                )
                );
    }

    private Mono<User> createNewUser(Map<String, Object> attributes, String provider, String providerId) {
        User user = new User();
        user.setEmail(attributes.get("email").toString());
        user.setFullName(attributes.get("name").toString());
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setUsername(attributes.get("email").toString());
        user.setAvatarUrl(attributes.get("picture").toString());
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

}
