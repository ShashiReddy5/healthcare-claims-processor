package com.shashireddy.claims.controller;

import com.shashireddy.claims.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues demo JWTs for a single hard-coded credential. A real deployment
 * swaps this for a call to an actual identity provider — everything
 * downstream (JwtService, the filter chain) stays the same either way.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String DEMO_USERNAME = "examiner";
    private static final String DEMO_PASSWORD = "demo-password";

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token) {
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!DEMO_USERNAME.equals(request.username()) || !DEMO_PASSWORD.equals(request.password())) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken(request.username())));
    }
}
