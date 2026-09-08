package com.typingquiz.dto;

import java.util.List;

/**
 * 分组维度复习统计DTO
 * 用于展示各分组的学习/复习状态
 */
public class GroupReviewDTO {
    
    private Long groupId;
    private String groupName;
    
    // 各状态数量（细化标签）
    private int newQuizCount;      // 新测验（NEW且未到期）
    private int pendingLearnCount; // 待学习（LEARNING且已到期）
    private int pendingRelearnCount; // 待重学（RELEARNING且已到期）
    private int pendingReviewCount; // 待复习（REVIEW且已到期）
    private int scheduledCount;    // 未到期（学习中/复习中但时间未到）
    private int suspendedCount;    // 已暂停
    
    // 向后兼容的字段（保留原字段名供旧代码使用）
    private int newCount;      // 映射到 pendingLearnCount
    private int learningCount; // 映射到 scheduledCount
    private int reviewCount;   // 映射到 pendingReviewCount
    private int relearningCount; // 映射到 pendingRelearnCount
    private int dueTodayCount; // 今日到期总数
    
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

    public int getNewQuizCount() {
        return newQuizCount;
    }

    public void setNewQuizCount(int newQuizCount) {
        this.newQuizCount = newQuizCount;
    }

    public int getPendingLearnCount() {
        return pendingLearnCount;
    }

    public void setPendingLearnCount(int pendingLearnCount) {
        this.pendingLearnCount = pendingLearnCount;
    }

    public int getPendingRelearnCount() {
        return pendingRelearnCount;
    }

    public void setPendingRelearnCount(int pendingRelearnCount) {
        this.pendingRelearnCount = pendingRelearnCount;
    }

    public int getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(int pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public int getScheduledCount() {
        return scheduledCount;
    }

    public void setScheduledCount(int scheduledCount) {
        this.scheduledCount = scheduledCount;
    }

    public int getSuspendedCount() {
        return suspendedCount;
    }

    public void setSuspendedCount(int suspendedCount) {
        this.suspendedCount = suspendedCount;
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
