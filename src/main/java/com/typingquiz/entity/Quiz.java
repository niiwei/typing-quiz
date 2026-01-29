package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测验实体类
 * 表示一个完整的打字测验,包含标题、描述、时间限制和关联的答案列表
 */
@Entity
@Table(name = "quiz")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 时间限制(秒), null表示无限制
     */
    @Column(name = "time_limit")
    private Integer timeLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 关联的答案列表
     * 使用级联操作,删除测验时自动删除所有答案
     */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    /**
     * 在持久化之前设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 构造函数
    public Quiz() {
    }

    public Quiz(String title, String description, Integer timeLimit) {
        this.title = title;
        this.description = description;
        this.timeLimit = timeLimit;
    }

    // 便捷方法:添加答案
    public void addAnswer(Answer answer) {
        answers.add(answer);
        answer.setQuiz(this);
    }

    // 便捷方法:移除答案
    public void removeAnswer(Answer answer) {
        answers.remove(answer);
        answer.setQuiz(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", timeLimit=" + timeLimit +
                ", createdAt=" + createdAt +
                ", answersCount=" + (answers != null ? answers.size() : 0) +
                '}';
    }
}
