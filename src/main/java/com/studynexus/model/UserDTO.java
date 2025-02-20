package com.studynexus.model;

import lombok.Data;

@Data
public class UserDTO {

    private String id;
    private String email;
    private String fullName;
    private String username;
    private String avatarUrl;

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        return dto;
    }
}
