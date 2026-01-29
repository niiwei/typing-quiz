package com.typingquiz.dto;

/**
 * 答案及其所属测验信息DTO
 * 用于数据库管理页面显示答案和测验的关联信息
 */
public class AnswerWithQuizDTO {
    private Long answerId;
    private String answerContent;
    private Long quizId;
    private String quizTitle;

    public AnswerWithQuizDTO() {
    }

    public AnswerWithQuizDTO(Long answerId, String answerContent, Long quizId, String quizTitle) {
        this.answerId = answerId;
        this.answerContent = answerContent;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
    }

    // Getters and Setters
    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }
}
