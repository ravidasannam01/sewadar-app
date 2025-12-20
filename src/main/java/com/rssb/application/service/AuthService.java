package com.rssb.application.service;

import com.rssb.application.dto.LoginRequest;
import com.rssb.application.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
    Boolean validateToken(String token);
}

