package com.typingquiz.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证答案响应对象
 */
public class ValidationResponse {
    
    private boolean valid;
    private Long answerId;
    private String displayContent;
    private boolean alreadyFound;
    private List<AnswerMatchDTO> matches = new ArrayList<>();

    public ValidationResponse() {
    }

    public ValidationResponse(boolean valid, Long answerId, String displayContent, boolean alreadyFound) {
        this.valid = valid;
        this.answerId = answerId;
        this.displayContent = displayContent;
        this.alreadyFound = alreadyFound;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public String getDisplayContent() {
        return displayContent;
    }

    public void setDisplayContent(String displayContent) {
        this.displayContent = displayContent;
    }

    public boolean isAlreadyFound() {
        return alreadyFound;
    }

    public void setAlreadyFound(boolean alreadyFound) {
        this.alreadyFound = alreadyFound;
    }

    public List<AnswerMatchDTO> getMatches() {
        return matches;
    }

    public void setMatches(List<AnswerMatchDTO> matches) {
        this.matches = matches == null ? new ArrayList<>() : matches;
    }
}
