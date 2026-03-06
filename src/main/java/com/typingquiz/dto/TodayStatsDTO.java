package com.typingquiz.dto;

/**
 * 今日学习统计数据传输对象
 * 对应Anki的"今天"统计页面
 */
public class TodayStatsDTO {

    private int totalCards;              // 总卡片数
    private double totalTimeMinutes;   // 总耗时（分钟）
    private int avgTimePerCard;          // 平均每卡耗时（秒）

    // 评级分布
    private int againCount;
    private double againPercent;
    private int hardCount;
    private int goodCount;
    private int easyCount;

    // 分类统计
    private int newCards;        // 新学卡片
    private int reviewCards;     // 复习卡片
    private int relearningCards; // 重学卡片

    // 连续天数
    private int streakDays;

    // 构造函数
    public TodayStatsDTO() {
    }

    /**
     * 创建空统计（今日无学习记录）
     */
    public static TodayStatsDTO empty() {
        return new TodayStatsDTO();
    }

    // Getters and Setters
    public int getTotalCards() {
        return totalCards;
    }

    public void setTotalCards(int totalCards) {
        this.totalCards = totalCards;
    }

    public double getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public void setTotalTimeMinutes(double totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public int getAvgTimePerCard() {
        return avgTimePerCard;
    }

    public void setAvgTimePerCard(int avgTimePerCard) {
        this.avgTimePerCard = avgTimePerCard;
    }

    public int getAgainCount() {
        return againCount;
    }

    public void setAgainCount(int againCount) {
        this.againCount = againCount;
    }

    public double getAgainPercent() {
        return againPercent;
    }

    public void setAgainPercent(double againPercent) {
        this.againPercent = againPercent;
    }

    public int getHardCount() {
        return hardCount;
    }

    public void setHardCount(int hardCount) {
        this.hardCount = hardCount;
    }

    public int getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(int goodCount) {
        this.goodCount = goodCount;
    }

    public int getEasyCount() {
        return easyCount;
    }

    public void setEasyCount(int easyCount) {
        this.easyCount = easyCount;
    }

    public int getNewCards() {
        return newCards;
    }

    public void setNewCards(int newCards) {
        this.newCards = newCards;
    }

    public int getReviewCards() {
        return reviewCards;
    }

    public void setReviewCards(int reviewCards) {
        this.reviewCards = reviewCards;
    }

    public int getRelearningCards() {
        return relearningCards;
    }

    public void setRelearningCards(int relearningCards) {
        this.relearningCards = relearningCards;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }
}
