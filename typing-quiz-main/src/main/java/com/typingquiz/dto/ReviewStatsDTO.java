package com.typingquiz.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 复习统计数据传输对象
 */
public class ReviewStatsDTO {

    // 卡片统计
    private int totalCards;           // 总卡片数
    private int newCards;             // 新卡片数
    private int learningCards;        // 学习中卡片数
    private int reviewCards;          // 待复习卡片数
    private int relearningCards;      // 重学中卡片数
    private int suspendedCards;       // 暂停卡片数

    // 今日统计
    private int dueToday;             // 今日到期数量

    // 累计统计
    private int totalReviewCount;     // 累计复习次数
    private int totalLapseCount;      // 累计遗忘次数
    private double averageEaseFactor; // 平均简易度系数

    // 连续学习天数
    private int streakDays;

    // 未来30天预测
    private List<DailyForecast> forecast;

    // 间隔分布
    private Map<String, Integer> intervalDistribution;

    // 简易度分布
    private Map<String, Integer> easeDistribution;

    // 复习历史（最近30天）
    private List<DailyReviewHistory> reviewHistory;

    // Getters and Setters
    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }

    public int getNewCards() { return newCards; }
    public void setNewCards(int newCards) { this.newCards = newCards; }

    public int getLearningCards() { return learningCards; }
    public void setLearningCards(int learningCards) { this.learningCards = learningCards; }

    public int getReviewCards() { return reviewCards; }
    public void setReviewCards(int reviewCards) { this.reviewCards = reviewCards; }

    public int getRelearningCards() { return relearningCards; }
    public void setRelearningCards(int relearningCards) { this.relearningCards = relearningCards; }

    public int getSuspendedCards() { return suspendedCards; }
    public void setSuspendedCards(int suspendedCards) { this.suspendedCards = suspendedCards; }

    public int getDueToday() { return dueToday; }
    public void setDueToday(int dueToday) { this.dueToday = dueToday; }

    public int getTotalReviewCount() { return totalReviewCount; }
    public void setTotalReviewCount(int totalReviewCount) { this.totalReviewCount = totalReviewCount; }

    public int getTotalLapseCount() { return totalLapseCount; }
    public void setTotalLapseCount(int totalLapseCount) { this.totalLapseCount = totalLapseCount; }

    public double getAverageEaseFactor() { return averageEaseFactor; }
    public void setAverageEaseFactor(double averageEaseFactor) { this.averageEaseFactor = averageEaseFactor; }

    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

    public List<DailyForecast> getForecast() { return forecast; }
    public void setForecast(List<DailyForecast> forecast) { this.forecast = forecast; }

    public Map<String, Integer> getIntervalDistribution() { return intervalDistribution; }
    public void setIntervalDistribution(Map<String, Integer> intervalDistribution) { this.intervalDistribution = intervalDistribution; }

    public Map<String, Integer> getEaseDistribution() { return easeDistribution; }
    public void setEaseDistribution(Map<String, Integer> easeDistribution) { this.easeDistribution = easeDistribution; }

    public List<DailyReviewHistory> getReviewHistory() { return reviewHistory; }
    public void setReviewHistory(List<DailyReviewHistory> reviewHistory) { this.reviewHistory = reviewHistory; }

    /**
     * 每日预测数据
     */
    public static class DailyForecast {
        private LocalDate date;
        private int count;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    /**
     * 每日复习历史记录
     */
    public static class DailyReviewHistory {
        private LocalDate date;
        private int reviewedCount;
        private int correctCount;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public int getReviewedCount() { return reviewedCount; }
        public void setReviewedCount(int reviewedCount) { this.reviewedCount = reviewedCount; }

        public int getCorrectCount() { return correctCount; }
        public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    }
}
