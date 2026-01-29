package com.typingquiz.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测验分组数据传输对象
 */
public class QuizGroupDTO {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private List<Long> quizIds;
    private LocalDateTime createdAt;

    public QuizGroupDTO() {
    }

    public QuizGroupDTO(Long id, String name, String description, Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<Long> getQuizIds() {
        return quizIds;
    }

    public void setQuizIds(List<Long> quizIds) {
        this.quizIds = quizIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
