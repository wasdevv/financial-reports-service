package com.financialreports.dto;

import com.financialreports.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    // Sem campo de papel: um "role": "ADMIN" no JSON é simplesmente ignorado.
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                    message = "only letters, numbers, dot, dash and underscore") String username,
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password) {}

    public record UserResponse(Long id, String username, String email, String fullName, String role, boolean active) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isActive());
        }
    }

    public record TokenResponse(String token, String type, long expiresIn, UserResponse user) {}
}
