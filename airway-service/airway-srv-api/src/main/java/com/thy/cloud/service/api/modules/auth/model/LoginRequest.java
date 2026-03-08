package com.thy.cloud.service.api.modules.auth.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
