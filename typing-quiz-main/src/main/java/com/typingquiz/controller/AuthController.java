package com.typingquiz.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.typingquiz.dto.LoginRequest;
import com.typingquiz.dto.RegisterRequest;
import com.typingquiz.entity.User;
import com.typingquiz.service.UserService;
import com.typingquiz.util.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            response.put("success", true);
            response.put("token", token);
            response.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        return userService.login(request.getUsername(), request.getPassword())
            .map(user -> {
                String token = JwtUtil.generateToken(user.getId(), user.getUsername());
                response.put("success", true);
                response.put("token", token);
                response.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                response.put("success", false);
                response.put("message", "用户名或密码错误");
                return ResponseEntity.badRequest().body(response);
            });
    }
}
