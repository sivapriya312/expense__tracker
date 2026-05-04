package com.example.backend.dto;

public record AuthResponse(Long userId, String name, String email, String token) {
}
