package com.typingquiz.util;

import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtil {

    // 从环境变量获取JWT密钥，默认仅用于开发环境
    private static final String SECRET = System.getenv().getOrDefault("JWT_SECRET", "dev_secret_key_change_in_production");
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
