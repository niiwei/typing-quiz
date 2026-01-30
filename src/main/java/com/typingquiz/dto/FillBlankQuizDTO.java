package com.typingquiz.dto;

import java.util.List;

/**
 * 填空题数据传输对象
 */
public class FillBlankQuizDTO {

    private Long id;
    private Long quizId;
    private String fullText;
    private String displayText;
    private List<BlankInfo> blanks;
    private Integer blanksCount;

    /**
     * 挖空信息内部类
     */
    public static class BlankInfo {
        private Integer startIndex;
        private Integer endIndex;
        private String correctAnswer;
        private String comment;

        public BlankInfo() {
        }

        public BlankInfo(Integer startIndex, Integer endIndex, String correctAnswer) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.correctAnswer = correctAnswer;
        }

        public Integer getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(Integer startIndex) {
            this.startIndex = startIndex;
        }

        public Integer getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(Integer endIndex) {
            this.endIndex = endIndex;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public FillBlankQuizDTO() {
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

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public List<BlankInfo> getBlanks() {
        return blanks;
    }

    public void setBlanks(List<BlankInfo> blanks) {
        this.blanks = blanks;
    }

    public Integer getBlanksCount() {
        return blanksCount;
    }

    public void setBlanksCount(Integer blanksCount) {
        this.blanksCount = blanksCount;
    }
}
