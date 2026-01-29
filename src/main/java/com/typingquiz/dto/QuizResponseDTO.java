package com.typingquiz.dto;

import com.typingquiz.entity.QuizType;
import java.time.LocalDateTime;

/**
 * 测验响应对象
 * 用于返回测验详情
 */
public class QuizResponseDTO {
    
    private Long id;
    private String title;
    private String description;
    private Integer timeLimit;
    private Integer totalAnswers;
    private LocalDateTime createdAt;
    private QuizType quizType;
    private FillBlankQuizDTO fillBlankQuiz;

    public QuizResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getTotalAnswers() {
        return totalAnswers;
    }

    public void setTotalAnswers(Integer totalAnswers) {
        this.totalAnswers = totalAnswers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }

    public FillBlankQuizDTO getFillBlankQuiz() {
        return fillBlankQuiz;
    }

    public void setFillBlankQuiz(FillBlankQuizDTO fillBlankQuiz) {
        this.fillBlankQuiz = fillBlankQuiz;
    }
}
