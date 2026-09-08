package com.typingquiz.dto;

import java.util.ArrayList;
import java.util.List;

public class AnswerPartDTO {
    private List<AnswerSegmentDTO> segments = new ArrayList<>();

    public List<AnswerSegmentDTO> getSegments() {
        return segments;
    }

    public void setSegments(List<AnswerSegmentDTO> segments) {
        this.segments = segments;
    }
}
