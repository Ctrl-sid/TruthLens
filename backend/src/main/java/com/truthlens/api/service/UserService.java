package com.truthlens.api.service;

import com.truthlens.api.config.JwtTokenProvider;
import com.truthlens.api.dto.AuthDTO;
import com.truthlens.api.model.User;
import com.truthlens.api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostConstruct
    public void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@truthlens.ai")
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("TruthLens Admin Superuser")
                    .role("ROLE_ADMIN")
                    .status("ACTIVE")
                    .build();
            userRepository.save(admin);
            System.out.println(">>> SEEDED ADMIN SUPERUSER: admin / Admin@123 <<<");
        }
    }

    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user != null && "BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Access Denied: Your account has been suspended/banned due to code of conduct violations.");
        }

        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = tokenProvider.generateToken(authentication);

            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .build();
        } catch (org.springframework.security.authentication.DisabledException | 
                 org.springframework.security.authentication.LockedException e) {
            throw new IllegalArgumentException("Access Denied: Your account has been suspended/banned due to code of conduct violations.");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid username or password");
        }
    }

    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role("ROLE_USER")
                .status("ACTIVE")
                .build();

        userRepository.save(user);

        String token = tokenProvider.generateTokenFromUsername(user.getUsername());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
