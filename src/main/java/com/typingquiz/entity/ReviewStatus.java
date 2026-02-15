package com.typingquiz.entity;

/**
 * 测验复习状态枚举
 * 对应Anki的卡片状态概念
 */
public enum ReviewStatus {
    
    /**
     * 新测验，从未学习过
     */
    NEW("新测验"),
    
    /**
     * 学习中，正在通过初学间隔阶段
     */
    LEARNING("学习中"),
    
    /**
     * 待复习，已完成学习进入复习阶段
     */
    REVIEW("待复习"),
    
    /**
     * 重学中，复习时遗忘后重新学习
     */
    RELEARNING("重学中"),
    
    /**
     * 暂停，长期不参与复习
     */
    SUSPENDED("已暂停");
    
    private final String displayName;
    
    ReviewStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 检查状态是否处于活跃学习状态（非暂停非搁置）
     */
    public boolean isActive() {
        return this != SUSPENDED;
    }
    
    /**
     * 检查状态是否需要显示在今日复习列表中
     */
    public boolean isStudyable() {
        return this == NEW || this == LEARNING || this == REVIEW || this == RELEARNING;
    }
}
