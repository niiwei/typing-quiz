package com.typingquiz.controller;

import com.typingquiz.dto.PersonalAccessTokenDTO;
import com.typingquiz.config.ApiAuthenticationFilter;
import com.typingquiz.service.PersonalAccessTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personal-access-tokens")
public class PersonalAccessTokenController {
    private final PersonalAccessTokenService service;

    public PersonalAccessTokenController(PersonalAccessTokenService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PersonalAccessTokenDTO>> list(HttpServletRequest request) {
        Long userId = jwtUserId(request);
        return userId == null ? ResponseEntity.status(401).build() : ResponseEntity.ok(service.list(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = jwtUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.status(201).body(service.create(userId, body.get("name")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("code", "INVALID_ARGUMENT", "message", e.getMessage(), "details", Map.of()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revoke(@PathVariable Long id, HttpServletRequest request) {
        Long userId = jwtUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            service.revoke(userId, id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Long jwtUserId(HttpServletRequest request) {
        if (!"JWT".equals(request.getAttribute(ApiAuthenticationFilter.TOKEN_TYPE_ATTRIBUTE))) return null;
        return (Long) request.getAttribute(ApiAuthenticationFilter.USER_ID_ATTRIBUTE);
    }
}
