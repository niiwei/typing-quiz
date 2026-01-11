package com.typingquiz.dto;

/**
 * 验证答案请求对象
 */
public class ValidationRequest {
    
    private Long quizId;
    private String input;

    public ValidationRequest() {
    }

    public ValidationRequest(Long quizId, String input) {
        this.quizId = quizId;
        this.input = input;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
