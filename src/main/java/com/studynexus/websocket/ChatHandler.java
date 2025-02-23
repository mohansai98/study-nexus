package com.studynexus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studynexus.dto.ChatMessageDTO;
import com.studynexus.model.ChatMessage;
import com.studynexus.security.JWTUtil;
import com.studynexus.service.ChatService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler implements WebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final JWTUtil jwtUtil;

    // Store active sessions by user ID
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // Store room memberships: roomId -> Set of userIds
    private static final Map<String, Set<String>> roomMembers = new ConcurrentHashMap<>();

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        String userId = validateAndGetUserId(session);
        if (userId == null) {
            return session.close(); // Invalid token, close connection
        }

        // Remove any existing session for this user
        WebSocketSession existingSession = userSessions.get(userId);
        if (existingSession != null && existingSession.isOpen()) {
            return existingSession.close().then(Mono.empty());
        }

        // Store new session
        userSessions.put(userId, session);

        return session.receive()
                .flatMap(message -> {
                    // Retain the message before processing
                    message.retain();
                    return handleIncomingMessage(userId, message)
                            .doFinally(signalType -> {
                                try {
                                    // Release the message after processing
                                    message.release();
                                } catch (Exception e) {
                                    log.warn("Error releasing message: {}", e.getMessage());
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing message from user {}: {}", userId, e.getMessage());
                                return Mono.empty();
                            });
                })
                .doFinally(signalType -> cleanup(userId))
                .then();
    }

    private String validateAndGetUserId(WebSocketSession session) {
        try {
            String query = session.getHandshakeInfo().getUri().getQuery();
            String token = (query != null && query.startsWith("token=")) ?
                    query.substring(6) : null;

            if (token == null) {
                log.warn("No token provided in WebSocket connection");
                return null;
            }

            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return null;
        }
    }

    private Mono<Void> handleIncomingMessage(String userId, WebSocketMessage message) {
        return Mono.fromCallable(() -> objectMapper.readValue(message.getPayloadAsText(), ChatMessageDTO.class))
                .flatMap(messageDTO -> {
                    try {
                        ChatMessage.MessageType type = ChatMessage.MessageType.valueOf(messageDTO.getType());
                        return switch (type) {
                            case JOIN_ROOM -> handleJoinRoom(userId, messageDTO.getRoomId());
                            case MESSAGE -> handleChatMessage(userId, messageDTO);
                            default -> {
                                log.warn("Unsupported message type: {}", type);
                                yield Mono.empty();
                            }
                        };
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid message type: {}", messageDTO.getType());
                        return Mono.empty();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> handleJoinRoom(String userId, String roomId) {
        if (!chatService.isValidRoomMember(userId, roomId)) {
            log.warn("User {} attempted to join unauthorized room {}", userId, roomId);
            return Mono.empty();
        }

        roomMembers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);

        ChatMessageDTO joinMessage = ChatMessageDTO.builder()
                .type("ROOM_JOINED")
                .roomId(roomId)
                .build();

        return sendMessageToUser(userId, joinMessage);
    }

    private Mono<Void> handleChatMessage(String userId, ChatMessageDTO messageDTO) {
        // Validate user is member of the room
        if (!isUserInRoom(userId, messageDTO.getRoomId())) {
            log.warn("User {} attempted to send message to unauthorized room {}",
                    userId, messageDTO.getRoomId());
            return Mono.empty();
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.MESSAGE)
                .roomId(messageDTO.getRoomId())
                .senderId(userId)
                .content(messageDTO.getContent())
                .build();

        return chatService.saveMessage(chatMessage)
                .flatMap(saved -> {
                    ChatMessageDTO dto = ChatMessageDTO.convertToDTO(saved);
                    Set<String> members = roomMembers.get(messageDTO.getRoomId());

                    if (members != null && !members.isEmpty()) {
                        return Mono.when(
                                members.stream()
                                        .map(memberId -> sendMessageToUser(memberId, dto))
                                        .toList()
                        );
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> sendMessageToUser(String userId, ChatMessageDTO message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                return session.send(Mono.just(session.textMessage(messageJson)))
                        .onErrorResume(e -> {
                            log.error("Error sending message to user {}: {}", userId, e.getMessage());
                            return Mono.empty();
                        });
            } catch (Exception e) {
                log.error("Error serializing message: {}", e.getMessage());
            }
        }
        return Mono.empty();
    }

    private boolean isUserInRoom(String userId, String roomId) {
        return chatService.isValidRoomMember(userId, roomId);
    }

    private void cleanup(String userId) {
        log.info("Cleaning up resources for user: {}", userId);
        userSessions.remove(userId);
        roomMembers.values().forEach(members -> members.remove(userId));
    }
}