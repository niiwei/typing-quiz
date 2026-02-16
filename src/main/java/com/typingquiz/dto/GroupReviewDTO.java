package com.typingquiz.dto;

import java.util.List;

/**
 * 分组维度复习统计DTO
 * 用于展示各分组的学习/复习状态
 */
public class GroupReviewDTO {
    
    private Long groupId;
    private String groupName;
    
    // 各状态数量
    private int newCount;      // 新测验
    private int learningCount; // 学习中
    private int reviewCount;   // 待复习
    private int relearningCount; // 重新学习中
    private int dueTodayCount; // 今日到期
    
    // 是否可展开（包含测验详情）
    private boolean expandable;
    
    // 测验列表（展开时填充）
    private List<QuizReviewItemDTO> quizzes;

    public GroupReviewDTO() {
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getNewCount() {
        return newCount;
    }

    public void setNewCount(int newCount) {
        this.newCount = newCount;
    }

    public int getLearningCount() {
        return learningCount;
    }

    public void setLearningCount(int learningCount) {
        this.learningCount = learningCount;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getRelearningCount() {
        return relearningCount;
    }

    public void setRelearningCount(int relearningCount) {
        this.relearningCount = relearningCount;
    }

    public int getDueTodayCount() {
        return dueTodayCount;
    }

    public void setDueTodayCount(int dueTodayCount) {
        this.dueTodayCount = dueTodayCount;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public List<QuizReviewItemDTO> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<QuizReviewItemDTO> quizzes) {
        this.quizzes = quizzes;
    }

    /**
     * 获取总计数
     */
    public int getTotalCount() {
        return newCount + learningCount + reviewCount;
    }

    @Override
    public String toString() {
        return "GroupReviewDTO{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", newCount=" + newCount +
                ", learningCount=" + learningCount +
                ", reviewCount=" + reviewCount +
                ", dueTodayCount=" + dueTodayCount +
                '}';
    }
}
