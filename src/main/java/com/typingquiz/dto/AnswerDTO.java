package com.typingquiz.dto;

/**
 * 答案数据传输对象
 * 用于答案响应
 */
public class AnswerDTO {
    
    private Long id;
    private String content;
    private String comment;

    public AnswerDTO() {
    }

    public AnswerDTO(Long id, String content) {
        this.id = id;
        this.content = content;
    }

    public AnswerDTO(Long id, String content, String comment) {
        this.id = id;
        this.content = content;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
