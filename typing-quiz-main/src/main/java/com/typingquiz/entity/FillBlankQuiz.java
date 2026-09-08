package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 填空题实体类
 * 存储填空题的完整文本、挖空信息等
 */
@Entity
@Table(name = "fill_blank_quiz")
public class FillBlankQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的测验ID
     */
    @Column(name = "quiz_id", nullable = false, unique = true)
    private Long quizId;

    /**
     * 完整的原始文本（未挖空）
     */
    @Column(name = "full_text", nullable = false, columnDefinition = "TEXT")
    private String fullText;

    /**
     * 挖空信息的JSON格式
     * 存储挖空位置和正确答案
     * 格式: [{"startIndex": 0, "endIndex": 5, "correctAnswer": "北京"}]
     */
    @Column(name = "blanks_info", nullable = false, columnDefinition = "TEXT")
    private String blanksInfo;

    /**
     * 显示用的文本（用___替换挖空部分）
     */
    @Column(name = "display_text", nullable = false, columnDefinition = "TEXT")
    private String displayText;

    /**
     * 挖空数量
     */
    @Column(name = "blanks_count", nullable = false)
    private Integer blanksCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 构造函数
    public FillBlankQuiz() {
    }

    public FillBlankQuiz(Long quizId, String fullText, String blanksInfo, String displayText, Integer blanksCount) {
        this.quizId = quizId;
        this.fullText = fullText;
        this.blanksInfo = blanksInfo;
        this.displayText = displayText;
        this.blanksCount = blanksCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public String getBlanksInfo() {
        return blanksInfo;
    }

    public void setBlanksInfo(String blanksInfo) {
        this.blanksInfo = blanksInfo;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public Integer getBlanksCount() {
        return blanksCount;
    }

    public void setBlanksCount(Integer blanksCount) {
        this.blanksCount = blanksCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FillBlankQuiz{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", blanksCount=" + blanksCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
