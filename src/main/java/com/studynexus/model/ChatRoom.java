package com.studynexus.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Builder
@Document(collection = "chat_rooms")
public class ChatRoom {
    @Id
    private String id;
    private String name;
    private Set<String> participants;
    private RoomType type;

    public enum RoomType {
        DIRECT,
        GROUP
    }
}
