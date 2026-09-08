package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测验分组实体类
 * 表示一个测验分组,用于组织和管理测验
 */
@Entity
@Table(name = "quiz_group")
public class QuizGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 分组所有者ID，用于账户隔离
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 排序顺序,用于自定义分组显示顺序
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 关联的测验列表
     * 使用级联操作,删除分组时自动解除关联(不删除测验)
     */
    @ManyToMany
    @JoinTable(
        name = "quiz_group_quiz",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "quiz_id")
    )
    private List<Quiz> quizzes = new ArrayList<>();

    /**
     * 在持久化之前设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 构造函数
    public QuizGroup() {
    }

    public QuizGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public QuizGroup(String name, String description, Long userId) {
        this.name = name;
        this.description = description;
        this.userId = userId;
    }

    // 便捷方法:添加测验
    public void addQuiz(Quiz quiz) {
        if (!quizzes.contains(quiz)) {
            quizzes.add(quiz);
        }
    }

    // 便捷方法:移除测验
    public void removeQuiz(Quiz quiz) {
        quizzes.remove(quiz);
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    public int getQuizCount() {
        return quizzes != null ? quizzes.size() : 0;
    }

    @Override
    public String toString() {
        return "QuizGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", displayOrder=" + displayOrder +
                ", quizCount=" + getQuizCount() +
                '}';
    }
}
