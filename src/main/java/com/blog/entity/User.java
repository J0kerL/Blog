package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String avatar;
    private String bio;
    private String role;          // ROLE_USER / ROLE_ADMIN
    private Integer status;       // 0=禁用 1=正常
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
