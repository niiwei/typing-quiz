package com.typingquiz.dto;

import java.util.List;

/**
 * 答案数据传输对象
 * 用于答案响应
 */
public class AnswerDTO {
    
    private Long id;
    private String content;
    private String comment;
    private Integer formatVersion;
    private List<AnswerPartDTO> parts;

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

    public Integer getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(Integer formatVersion) {
        this.formatVersion = formatVersion;
    }

    public List<AnswerPartDTO> getParts() {
        return parts;
    }

    public void setParts(List<AnswerPartDTO> parts) {
        this.parts = parts;
    }
}
