package com.typingquiz.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 复习统计数据传输对象
 */
@Data
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

    /**
     * 每日预测数据
     */
    @Data
    public static class DailyForecast {
        private LocalDate date;
        private int count;
    }

    /**
     * 每日复习历史记录
     */
    @Data
    public static class DailyReviewHistory {
        private LocalDate date;
        private int reviewedCount;   // 当日复习数量
        private int correctCount;    // 当日正确数量（用于自动评级功能）
    }
}
