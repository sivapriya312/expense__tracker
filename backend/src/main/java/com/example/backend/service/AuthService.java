package com.example.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;

	public AuthService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public AuthResponse register(RegisterRequest request) {
		requireText(request.name(), "Name is required");
		requireText(request.email(), "Email is required");
		requireText(request.password(), "Password is required");

		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
		}

		User user = new User();
		user.setName(request.name().trim());
		user.setEmail(email);
		user.setPasswordHash(hashPassword(request.password()));
		user.setAuthToken(generateToken());

		return toAuthResponse(userRepository.save(user));
	}

	public AuthResponse login(LoginRequest request) {
		requireText(request.email(), "Email is required");
		requireText(request.password(), "Password is required");

		User user = userRepository.findByEmail(request.email().trim().toLowerCase())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		if (!user.getPasswordHash().equals(hashPassword(request.password()))) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		user.setAuthToken(generateToken());
		return toAuthResponse(userRepository.save(user));
	}

	public User requireUser(String token) {
		requireText(token, "Authorization token is required");
		String cleanToken = token.replace("Bearer ", "").trim();
		return userRepository.findByAuthToken(cleanToken)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid authorization token"));
	}

	private AuthResponse toAuthResponse(User user) {
		return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getAuthToken());
	}

	private void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, message);
		}
	}

	private String generateToken() {
		return UUID.randomUUID().toString();
	}

	private String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
