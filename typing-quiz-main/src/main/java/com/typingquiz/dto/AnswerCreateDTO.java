package com.typingquiz.dto;

import java.util.List;

/**
 * 答案创建数据传输对象
 * 用于创建测验时的答案数据
 */
public class AnswerCreateDTO {
    
    private String content;
    private String comment;
    private Integer formatVersion;
    private List<AnswerPartDTO> parts;

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
