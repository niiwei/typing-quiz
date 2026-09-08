package com.typingquiz.dto;

/**
 * 答案创建数据传输对象
 * 用于创建测验时的答案数据
 */
public class AnswerCreateDTO {
    
    private String content;
    private String comment;

    public AnswerCreateDTO() {
    }

    public AnswerCreateDTO(String content, String comment) {
        this.content = content;
        this.comment = comment;
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
