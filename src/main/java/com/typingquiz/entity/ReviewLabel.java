package com.typingquiz.entity;

/**
 * 复习标签体系 - 业务层动态标签
 * 用于前端展示和统一判断逻辑
 */
public enum ReviewLabel {
    PENDING_LEARN("待学习", "NEW或LEARNING且已到期"),
    PENDING_REVIEW("待复习", "REVIEW或RELEARNING且已到期"),
    SCHEDULED("未到期", "学习中/复习中/重学中但未到复习时间"),
    SUSPENDED("已暂停", "用户主动暂停的测验");

    private final String displayName;
    private final String description;

    ReviewLabel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为今日待处理（待学习或待复习）
     */
    public boolean isDueToday() {
        return this == PENDING_LEARN || this == PENDING_REVIEW;
    }
}
