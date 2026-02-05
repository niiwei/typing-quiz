package com.typingquiz.util;

import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtil {

    private static final String SECRET = "typingquizsecretkey2024";
    private static final long EXPIRATION = 86400000L;

    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(userId + ":" + username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        String subject = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody().getSubject();
        return subject.substring(subject.indexOf(":") + 1);
    }

    public static Long getUserIdFromToken(String token) {
        String subject = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody().getSubject();
        return Long.parseLong(subject.substring(0, subject.indexOf(":")));
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
