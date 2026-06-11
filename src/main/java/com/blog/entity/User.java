package com.blog.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
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
