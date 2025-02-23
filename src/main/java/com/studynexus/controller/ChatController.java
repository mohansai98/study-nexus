package com.studynexus.controller;

import com.studynexus.dto.ChatMessageDTO;
import com.studynexus.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/direct-room")
    public Mono<ResponseEntity<Map<String, String>>> getDirectRoom(
            @AuthenticationPrincipal OAuth2AuthenticationToken authentication,
            @RequestParam String user1,
            @RequestParam String user2
    ) {
        if (authentication == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        }
        return Mono.zip(chatService.userExists(user1), chatService.userExists(user2))
                .flatMap(tuple -> {
                    Boolean user1Exists = tuple.getT1();
                    Boolean user2Exists = tuple.getT2();

                    if (!user1Exists || !user2Exists) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "One or both users do not exist")));
                    }

                    String roomId = chatService.generateDirectRoomId(user1, user2);
                    return Mono.just(ResponseEntity.ok(Map.of("roomId", roomId)));
                });
    }

    @GetMapping("/history/{roomId}")
    public Flux<ChatMessageDTO> getChatHistory(@AuthenticationPrincipal OAuth2AuthenticationToken authentication,
                                               @PathVariable String roomId) {
        if (authentication == null) {
            return Flux.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        }

        String email = authentication.getPrincipal().getAttribute("email");

        return chatService.getUserByEmail(email)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMapMany(user -> {
                    String userId = user.getId();
                    String[] roomUsers = roomId.split("_");

                    // Check if the current user's ID is part of the room ID
                    if (roomUsers.length != 2 || (!roomUsers[0].equals(userId) && !roomUsers[1].equals(userId))) {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this chat room"));
                    }

                    return chatService.getRoomHistory(roomId)
                            .map(ChatMessageDTO::convertToDTO);
                });
    }
}
