package com.studynexus.repository;

import com.studynexus.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByEmail(String email);
    Mono<User> findByProviderAndProviderId(String provider, String providerId);
    Mono<Boolean> existsByEmail(String email);
}
