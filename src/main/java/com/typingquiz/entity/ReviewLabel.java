package com.typingquiz.entity;

/**
 * 复习标签体系 - 业务层动态标签
 * 用于前端展示和统一判断逻辑
 */
public enum ReviewLabel {
    PENDING_LEARN("待学习", "今日需要学习的测验"),
    PENDING_REVIEW("待复习", "今日需要复习的测验"),
    LEARNING("学习中", "正在学习阶段，未到复习时间"),
    REVIEWING("复习中", "正常复习周期，未到复习时间"),
    RELEARNING("重学中", "复习失败后重新学习"),
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
