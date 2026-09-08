package com.typingquiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.entity.EventLog;
import com.typingquiz.repository.EventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 埋点追踪服务
 * 用于记录用户行为事件
 */
@Service
public class TrackService {

    private static final Logger logger = LoggerFactory.getLogger(TrackService.class);

    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public TrackService(EventLogRepository eventLogRepository, ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录事件（异步执行，不阻塞主流程）
     *
     * @param userId    用户ID
     * @param eventType 事件类型
     * @param data      事件数据
     */
    @Async
    public void trackEvent(Long userId, String eventType, Map<String, Object> data) {
        try {
            String eventData = objectMapper.writeValueAsString(data);
            EventLog eventLog = new EventLog(userId, eventType, eventData);
            eventLogRepository.save(eventLog);
            logger.debug("埋点记录成功: userId={}, eventType={}", userId, eventType);
        } catch (Exception e) {
            // 埋点失败不影响主业务
            logger.warn("埋点记录失败: userId={}, eventType={}, error={}", userId, eventType, e.getMessage());
        }
    }

    /**
     * 记录事件（带会话ID和页面路径）
     */
    @Async
    public void trackEvent(Long userId, String eventType, Map<String, Object> data, String sessionId, String pagePath) {
        try {
            String eventData = objectMapper.writeValueAsString(data);
            EventLog eventLog = new EventLog(userId, eventType, eventData, sessionId, pagePath);
            eventLogRepository.save(eventLog);
            logger.debug("埋点记录成功: userId={}, eventType={}, sessionId={}", userId, eventType, sessionId);
        } catch (Exception e) {
            logger.warn("埋点记录失败: userId={}, eventType={}, error={}", userId, eventType, e.getMessage());
        }
    }

    /**
     * 记录复习评级事件（专门用于 review_rating_submit）
     */
    @Async
    public void trackReviewRating(Long userId, Long quizId, int rating, int timeSpent,
                                   String statusBefore, int intervalAfter, int easeFactor) {
        Map<String, Object> data = Map.of(
            "quizId", quizId,
            "rating", rating,
            "timeSpent", timeSpent,
            "statusBefore", statusBefore,
            "intervalAfter", intervalAfter,
            "easeFactor", easeFactor
        );
        trackEvent(userId, "review_rating_submit", data);
    }

    /**
     * 记录测验创建事件
     */
    @Async
    public void trackQuizCreated(Long userId, Long quizId, String quizType, int answerCount, String source) {
        Map<String, Object> data = Map.of(
            "quizId", quizId,
            "quizType", quizType,
            "answerCount", answerCount,
            "source", source
        );
        trackEvent(userId, "quiz_created", data);
    }

    /**
     * 记录页面访问
     */
    @Async
    public void trackPageView(Long userId, String pagePath, String referrer, String userAgent) {
        Map<String, Object> data = Map.of(
            "pagePath", pagePath,
            "referrer", referrer != null ? referrer : "",
            "userAgent", userAgent != null ? userAgent : ""
        );
        trackEvent(userId, "page_view", data);
    }

    /**
     * 记录用户会话开始
     */
    @Async
    public void trackSessionStart(Long userId, String entryPage, String source, String deviceType) {
        Map<String, Object> data = Map.of(
            "entryPage", entryPage,
            "source", source != null ? source : "direct",
            "deviceType", deviceType != null ? deviceType : "unknown"
        );
        trackEvent(userId, "user_session_start", data);
    }

    /**
     * 记录复习会话开始
     */
    @Async
    public void trackReviewSessionStart(Long userId, String mode, int newCount, int reviewCount, int relearningCount) {
        Map<String, Object> data = Map.of(
            "mode", mode,
            "newCount", newCount,
            "reviewCount", reviewCount,
            "relearningCount", relearningCount
        );
        trackEvent(userId, "review_session_start", data);
    }

    /**
     * 记录复习会话结束
     */
    @Async
    public void trackReviewSessionEnd(Long userId, int totalCards, int totalTimeSec, String exitType) {
        Map<String, Object> data = Map.of(
            "totalCards", totalCards,
            "totalTimeSec", totalTimeSec,
            "exitType", exitType
        );
        trackEvent(userId, "review_session_end", data);
    }
}
