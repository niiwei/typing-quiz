package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 事件日志实体类
 * 用于记录详细的用户行为埋点
 */
@Entity
@Table(name = "event_logs",
       indexes = {
           @Index(name = "idx_event_user_time", columnList = "user_id, created_at"),
           @Index(name = "idx_event_type", columnList = "event_type")
       })
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID，确保数据隔离
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 事件类型
     * 如: review_rating_submit, quiz_created, page_view
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * 事件详细数据（JSON格式）
     */
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    /**
     * 会话ID（用于关联同一用户的操作序列）
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 页面路径
     */
    @Column(name = "page_path", length = 255)
    private String pagePath;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 构造函数
    public EventLog() {
    }

    public EventLog(Long userId, String eventType, String eventData) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public EventLog(Long userId, String eventType, String eventData, String sessionId, String pagePath) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.sessionId = sessionId;
        this.pagePath = pagePath;
    }

    // 生命周期回调
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
