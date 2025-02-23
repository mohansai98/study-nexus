package com.studynexus.dto;

import com.studynexus.model.ChatMessage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageDTO {
    private String type;
    private String roomId;
    private String senderId;
    private String content;
    private Long timestamp;

    public static ChatMessageDTO convertToDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .type(message.getType().toString())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp().toEpochMilli())
                .build();
    }
}
