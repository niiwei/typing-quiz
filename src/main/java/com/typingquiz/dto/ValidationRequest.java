package com.typingquiz.dto;

/**
 * 验证答案请求对象
 */
public class ValidationRequest {
    
    private Long quizId;
    private String input;
    private Boolean ignorePunctuation;
    private Boolean ignoreSpaces;
    private Boolean ignoreCase;

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

    public Boolean getIgnorePunctuation() {
        return ignorePunctuation;
    }

    public void setIgnorePunctuation(Boolean ignorePunctuation) {
        this.ignorePunctuation = ignorePunctuation;
    }

    public Boolean getIgnoreSpaces() {
        return ignoreSpaces;
    }

    public void setIgnoreSpaces(Boolean ignoreSpaces) {
        this.ignoreSpaces = ignoreSpaces;
    }

    public Boolean getIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(Boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
}
