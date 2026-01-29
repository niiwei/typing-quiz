package com.typingquiz.entity;

import javax.persistence.*;

/**
 * 答案实体类
 * 表示测验中的一个答案,包含原始内容和标准化内容(用于大小写不敏感匹配)
 */
@Entity
@Table(name = "answer", indexes = {
        @Index(name = "idx_quiz_id", columnList = "quiz_id"),
        @Index(name = "idx_normalized_content", columnList = "normalized_content")
})
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属测验
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /**
     * 答案原始内容(保留大小写)
     */
    @Column(nullable = false)
    private String content;

    /**
     * 标准化内容(小写,用于匹配)
     */
    @Column(name = "normalized_content", nullable = false)
    private String normalizedContent;

    /**
     * 在持久化和更新之前自动设置标准化内容
     */
    @PrePersist
    @PreUpdate
    protected void normalizeContent() {
        if (content != null) {
            this.normalizedContent = content.trim().toLowerCase();
        }
    }

    // 构造函数
    public Answer() {
    }

    public Answer(String content) {
        this.content = content;
        this.normalizedContent = content.trim().toLowerCase();
    }

    public Answer(Quiz quiz, String content) {
        this.quiz = quiz;
        this.content = content;
        this.normalizedContent = content.trim().toLowerCase();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        if (content != null) {
            this.normalizedContent = content.trim().toLowerCase();
        }
    }

    public String getNormalizedContent() {
        return normalizedContent;
    }

    public void setNormalizedContent(String normalizedContent) {
        this.normalizedContent = normalizedContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Answer)) return false;
        Answer answer = (Answer) o;
        return id != null && id.equals(answer.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Answer{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", normalizedContent='" + normalizedContent + '\'' +
                '}';
    }
}
