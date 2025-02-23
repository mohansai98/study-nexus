package com.studynexus.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "messages")
public class ChatMessage {
    @Id
    private String id;
    private String roomId;
    private String senderId;
    private String content;
    private Instant timestamp;
    private MessageType type;

    public enum MessageType {
        MESSAGE,
        JOIN_ROOM,
        USER_STATUS,
        ROOM_JOINED
    }
}
