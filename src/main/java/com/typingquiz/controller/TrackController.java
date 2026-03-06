package com.typingquiz.controller;

import com.typingquiz.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 埋点追踪控制器
 * 接收前端上报的事件数据
 */
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private static final Logger logger = LoggerFactory.getLogger(TrackController.class);

    private final TrackService trackService;

    @Autowired
    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    /**
     * 批量接收前端埋点事件
     */
    @PostMapping
    public ResponseEntity<?> trackEvents(@RequestBody List<TrackEventRequest> events,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);

            for (TrackEventRequest event : events) {
                // 使用请求中的userId或从token提取的userId
                Long eventUserId = event.getUserId() != null ? event.getUserId() : userId;

                if (eventUserId == null) {
                    logger.warn("埋点事件缺少用户ID，跳过: eventType={}", event.getEventType());
                    continue;
                }

                trackService.trackEvent(eventUserId, event.getEventType(),
                        event.getData(), event.getSessionId(), event.getPagePath());
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("处理埋点事件失败", e);
            // 埋点失败不影响主业务，返回成功
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 单条事件接收（简化版）
     */
    @PostMapping("/single")
    public ResponseEntity<?> trackSingleEvent(@RequestBody TrackEventRequest event,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            Long eventUserId = event.getUserId() != null ? event.getUserId() : userId;

            if (eventUserId == null) {
                return ResponseEntity.badRequest().body("缺少用户ID");
            }

            trackService.trackEvent(eventUserId, event.getEventType(),
                    event.getData(), event.getSessionId(), event.getPagePath());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("处理单条埋点事件失败", e);
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 记录页面访问
     */
    @PostMapping("/pageview")
    public ResponseEntity<?> trackPageView(@RequestBody PageViewRequest request,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.ok().build(); // 未登录用户不记录
            }

            trackService.trackPageView(userId, request.getPagePath(),
                    request.getReferrer(), request.getUserAgent());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("记录页面访问失败", e);
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 记录会话开始
     */
    @PostMapping("/session/start")
    public ResponseEntity<?> trackSessionStart(@RequestBody SessionStartRequest request,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                return ResponseEntity.ok().build();
            }

            trackService.trackSessionStart(userId, request.getEntryPage(),
                    request.getSource(), request.getDeviceType());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("记录会话开始失败", e);
            return ResponseEntity.ok().build();
        }
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!com.typingquiz.util.JwtUtil.validateToken(token)) {
            return null;
        }
        return com.typingquiz.util.JwtUtil.getUserIdFromToken(token);
    }

    // ========== 请求DTO类 ==========

    public static class TrackEventRequest {
        private Long userId;
        private String eventType;
        private Map<String, Object> data;
        private String sessionId;
        private String pagePath;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getPagePath() { return pagePath; }
        public void setPagePath(String pagePath) { this.pagePath = pagePath; }
    }

    public static class PageViewRequest {
        private String pagePath;
        private String referrer;
        private String userAgent;

        public String getPagePath() { return pagePath; }
        public void setPagePath(String pagePath) { this.pagePath = pagePath; }
        public String getReferrer() { return referrer; }
        public void setReferrer(String referrer) { this.referrer = referrer; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }

    public static class SessionStartRequest {
        private String entryPage;
        private String source;
        private String deviceType;

        public String getEntryPage() { return entryPage; }
        public void setEntryPage(String entryPage) { this.entryPage = entryPage; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    }
}
