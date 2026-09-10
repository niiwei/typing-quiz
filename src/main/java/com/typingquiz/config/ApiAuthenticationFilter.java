package com.typingquiz.config;

import com.typingquiz.service.PersonalAccessTokenService;
import com.typingquiz.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiAuthenticationFilter extends OncePerRequestFilter {
    public static final String USER_ID_ATTRIBUTE = "mindpop.userId";
    public static final String TOKEN_TYPE_ATTRIBUTE = "mindpop.tokenType";
    private final PersonalAccessTokenService personalAccessTokenService;

    public ApiAuthenticationFilter(PersonalAccessTokenService personalAccessTokenService) {
        this.personalAccessTokenService = personalAccessTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith(request.getContextPath() + "/api/")
                || path.startsWith(request.getContextPath() + "/api/auth/")
                || path.equals(request.getContextPath() + "/api/track")
                || path.startsWith(request.getContextPath() + "/api/track/")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        String raw = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        Long userId = null;
        String type = null;
        boolean agentPath = path.startsWith(request.getContextPath() + "/api/agent/v1/");
        if (raw != null && raw.startsWith("mp_pat_")) {
            if (agentPath) {
                userId = personalAccessTokenService.authenticate(raw);
                type = "PAT";
            }
        } else if (!agentPath && raw != null && JwtUtil.validateToken(raw)) {
            userId = JwtUtil.getUserIdFromToken(raw);
            type = "JWT";
        }

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"需要有效的登录凭证\",\"details\":{}}");
            return;
        }
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        request.setAttribute(TOKEN_TYPE_ATTRIBUTE, type);
        chain.doFilter(request, response);
    }
}
