package com.typingquiz.dto;

import com.typingquiz.entity.ReviewStatus;

/**
 * 学习阶段响应DTO
 * 用于返回学习评级后的结果和下次复习安排
 */
public class LearnResponseDTO {
    
    private Long quizId;
    private int rating;
    private boolean completed;
    private ReviewStatus newStatus;
    private Integer intervalDays;
    private Integer nextIntervalMinutes;
    private String message;
    
    // 下一张测验信息（如果有）
    private Long nextQuizId;
    private String nextQuizTitle;
    
    // 今日剩余数量
    private int remainingToday;

    public LearnResponseDTO() {
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public ReviewStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ReviewStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Integer getNextIntervalMinutes() {
        return nextIntervalMinutes;
    }

    public void setNextIntervalMinutes(Integer nextIntervalMinutes) {
        this.nextIntervalMinutes = nextIntervalMinutes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getNextQuizId() {
        return nextQuizId;
    }

    public void setNextQuizId(Long nextQuizId) {
        this.nextQuizId = nextQuizId;
    }

    public String getNextQuizTitle() {
        return nextQuizTitle;
    }

    public void setNextQuizTitle(String nextQuizTitle) {
        this.nextQuizTitle = nextQuizTitle;
    }

    public int getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(int remainingToday) {
        this.remainingToday = remainingToday;
    }

    @Override
    public String toString() {
        return "LearnResponseDTO{" +
                "quizId=" + quizId +
                ", rating=" + rating +
                ", completed=" + completed +
                ", newStatus=" + newStatus +
                ", intervalDays=" + intervalDays +
                ", message='" + message + '\'' +
                '}';
    }
}
