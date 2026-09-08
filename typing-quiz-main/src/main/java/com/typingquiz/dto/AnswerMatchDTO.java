package com.typingquiz.dto;

import java.util.ArrayList;
import java.util.List;

public class AnswerMatchDTO {
    private Long answerId;
    private String displayContent;
    private List<Integer> partIndices = new ArrayList<>();

    public AnswerMatchDTO() {
    }

    public AnswerMatchDTO(Long answerId, String displayContent, List<Integer> partIndices) {
        this.answerId = answerId;
        this.displayContent = displayContent;
        this.partIndices = partIndices;
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

    public List<Integer> getPartIndices() {
        return partIndices;
    }

    public void setPartIndices(List<Integer> partIndices) {
        this.partIndices = partIndices;
    }
}
