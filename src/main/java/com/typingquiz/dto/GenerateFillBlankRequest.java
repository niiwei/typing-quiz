package com.typingquiz.dto;

public class GenerateFillBlankRequest {
    
    private String noteContent;
    private String bookName;

    public GenerateFillBlankRequest() {
    }

    public GenerateFillBlankRequest(String noteContent, String bookName) {
        this.noteContent = noteContent;
        this.bookName = bookName;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
}
