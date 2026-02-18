package com.typingquiz.dto;

import com.typingquiz.entity.ReviewStatus;

/**
 * 复习阶段响应DTO
 * 用于返回复习评级后的结果
 */
public class ReviewResponseDTO {
    
    private Long quizId;
    private int rating;
    private boolean completed;
    private Long nextQuizId;  // 添加下一个测验ID字段
    private ReviewStatus newStatus;
    private Integer intervalDays;
    private Integer nextIntervalMinutes;
    private String message;
    
    // 统计信息
    private int remainingToday;
    private int easeFactor;

    public ReviewResponseDTO() {
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

    public Long getNextQuizId() {
        return nextQuizId;
    }

    public void setNextQuizId(Long nextQuizId) {
        this.nextQuizId = nextQuizId;
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

    public int getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(int remainingToday) {
        this.remainingToday = remainingToday;
    }

    public int getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(int easeFactor) {
        this.easeFactor = easeFactor;
    }

    @Override
    public String toString() {
        return "ReviewResponseDTO{" +
                "quizId=" + quizId +
                ", rating=" + rating +
                ", completed=" + completed +
                ", intervalDays=" + intervalDays +
                ", message='" + message + '\'' +
                '}';
    }
}
