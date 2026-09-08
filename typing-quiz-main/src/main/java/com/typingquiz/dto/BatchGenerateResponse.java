package com.typingquiz.dto;

import java.util.List;

public class BatchGenerateResponse {
    
    private boolean success;
    private String bookName;
    private int quizCount;
    private List<QuizDTO> quizzes;
    private String error;

    public BatchGenerateResponse() {
    }

    public static BatchGenerateResponse success(String bookName, List<QuizDTO> quizzes) {
        BatchGenerateResponse response = new BatchGenerateResponse();
        response.success = true;
        response.bookName = bookName;
        response.quizzes = quizzes;
        response.quizCount = quizzes != null ? quizzes.size() : 0;
        return response;
    }

    public static BatchGenerateResponse error(String error) {
        BatchGenerateResponse response = new BatchGenerateResponse();
        response.success = false;
        response.error = error;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public int getQuizCount() {
        return quizCount;
    }

    public void setQuizCount(int quizCount) {
        this.quizCount = quizCount;
    }

    public List<QuizDTO> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<QuizDTO> quizzes) {
        this.quizzes = quizzes;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
