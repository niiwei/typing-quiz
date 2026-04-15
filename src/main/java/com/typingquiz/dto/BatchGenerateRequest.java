package com.typingquiz.dto;

public class BatchGenerateRequest {
    
    private String noteText;

    public BatchGenerateRequest() {
    }

    public BatchGenerateRequest(String noteText) {
        this.noteText = noteText;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }
}
