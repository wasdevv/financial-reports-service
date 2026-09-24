package com.financialreports.service;

import com.financialreports.dto.AuthDtos.LoginRequest;
import com.financialreports.dto.AuthDtos.RegisterRequest;
import com.financialreports.dto.AuthDtos.TokenResponse;
import com.financialreports.dto.AuthDtos.UserResponse;
import com.financialreports.entity.User;
import com.financialreports.exception.ApiException;
import com.financialreports.repository.UserRepository;
import com.financialreports.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    // Hash de uma senha qualquer: email inexistente também paga um bcrypt, e o tempo de
    // resposta não revela quais emails têm conta.
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.dummyHash = encoder.encode("timing-equalizer");
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        boolean ok = encoder.matches(req.password(), user == null ? dummyHash : user.getPassword());
        if (user == null || !ok) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isActive()) {
            throw ApiException.forbidden("This account is disabled");
        }
        return token(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = req.email().trim();
        String username = req.username().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email already registered");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Username already taken");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(req.fullName().trim());
        user.setPassword(encoder.encode(req.password()));
        user.setRole(User.Role.ANALYST);
        users.saveAndFlush(user);
        audit.record(user, "USER_REGISTERED", "USER", user.getId(), null);
        return token(user);
    }

    private TokenResponse token(User user) {
        return new TokenResponse(jwt.issue(user), "Bearer", jwt.expiration().toSeconds(), UserResponse.from(user));
    }
}
