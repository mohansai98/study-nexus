package com.studynexus.service;

import com.studynexus.model.ChatMessage;
import com.studynexus.model.User;
import com.studynexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    public Mono<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Mono<Boolean> userExists(String userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        return mongoTemplate.exists(query, "users");
    }

    public String generateDirectRoomId(String user1Id, String user2Id) {
        return user1Id.compareTo(user2Id) < 0
                ? user1Id + "_" + user2Id
                : user2Id + "_" + user1Id;
    }

    public List<String> getRoomMembers(String roomId) {
        // For direct messages, extract user IDs from room ID
        return Arrays.asList(roomId.split("_"));
    }

    public Mono<ChatMessage> saveMessage(ChatMessage message) {
        message.setTimestamp(Instant.now());
        return mongoTemplate.save(message);
    }

    public Flux<ChatMessage> getRoomHistory(String roomId) {
        return mongoTemplate.find(
                Query.query(Criteria.where("roomId").is(roomId))
                        .with(Sort.by(Sort.Direction.ASC, "timestamp")),
                ChatMessage.class
        );
    }

    public boolean isValidRoomMember(String userId, String roomId) {
        List<String> members = getRoomMembers(roomId);
        return members != null && members.contains(userId);
    }
}