package com.truthlens.api.controller;

import com.truthlens.api.dto.AuthDTO;
import com.truthlens.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthResponse> login(@RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(@RequestBody AuthDTO.RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }
}
