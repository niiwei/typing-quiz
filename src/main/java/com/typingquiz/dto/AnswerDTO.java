package com.typingquiz.dto;

/**
 * 答案数据传输对象
 * 用于答案响应
 */
public class AnswerDTO {
    
    private Long id;
    private String content;

    public AnswerDTO() {
    }

    public AnswerDTO(Long id, String content) {
        this.id = id;
        this.content = content;
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
}
