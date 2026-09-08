package com.typingquiz.dto;

public class AnswerSegmentDTO {
    private String kind;
    private String text;

    public AnswerSegmentDTO() {
    }

    public AnswerSegmentDTO(String kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
