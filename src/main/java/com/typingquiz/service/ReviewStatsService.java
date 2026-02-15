package com.typingquiz.service;

import com.typingquiz.dto.ReviewStatsDTO;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.QuizReviewStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 复习统计服务
 * 提供复习相关的统计数据和图表数据
 */
@Service
@RequiredArgsConstructor
public class ReviewStatsService {

    private final QuizReviewStatusRepository reviewStatusRepository;

    /**
     * 获取用户复习统计概览
     */
    public ReviewStatsDTO getUserStats(Long userId) {
        List<QuizReviewStatus> allStatus = reviewStatusRepository.findByUserId(userId);

        ReviewStatsDTO stats = new ReviewStatsDTO();

        // 统计各状态数量
        Map<ReviewStatus, Long> statusCounts = allStatus.stream()
                .collect(Collectors.groupingBy(QuizReviewStatus::getStatus, Collectors.counting()));

        stats.setTotalCards(allStatus.size());
        stats.setNewCards(statusCounts.getOrDefault(ReviewStatus.NEW, 0L).intValue());
        stats.setLearningCards(statusCounts.getOrDefault(ReviewStatus.LEARNING, 0L).intValue());
        stats.setReviewCards(statusCounts.getOrDefault(ReviewStatus.REVIEW, 0L).intValue());
        stats.setRelearningCards(statusCounts.getOrDefault(ReviewStatus.RELEARNING, 0L).intValue());
        stats.setSuspendedCards(statusCounts.getOrDefault(ReviewStatus.SUSPENDED, 0L).intValue());

        // 今日到期数量
        LocalDate today = LocalDate.now();
        long dueToday = allStatus.stream()
                .filter(s -> s.getNextReviewDate() != null && !s.getNextReviewDate().isAfter(today))
                .filter(s -> s.getStatus() != ReviewStatus.SUSPENDED)
                .count();
        stats.setDueToday((int) dueToday);

        // 累计复习次数和遗忘次数
        int totalReviews = allStatus.stream().mapToInt(QuizReviewStatus::getReviewCount).sum();
        int totalLapses = allStatus.stream().mapToInt(QuizReviewStatus::getLapseCount).sum();
        stats.setTotalReviewCount(totalReviews);
        stats.setTotalLapseCount(totalLapses);

        // 平均简易度
        double avgEase = allStatus.stream()
                .filter(s -> s.getEaseFactor() > 0)
                .mapToInt(QuizReviewStatus::getEaseFactor)
                .average()
                .orElse(2500);
        stats.setAverageEaseFactor(avgEase / 1000.0);

        // 计算未来30天预测
        stats.setForecast(calculateForecast(allStatus, 30));

        // 计算间隔分布
        stats.setIntervalDistribution(calculateIntervalDistribution(allStatus));

        // 计算简易度分布
        stats.setEaseDistribution(calculateEaseDistribution(allStatus));

        return stats;
    }

    /**
     * 计算未来N天的复习预测
     */
    private List<ReviewStatsDTO.DailyForecast> calculateForecast(List<QuizReviewStatus> allStatus, int days) {
        List<ReviewStatsDTO.DailyForecast> forecast = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < days; i++) {
            LocalDate date = today.plusDays(i);
            long count = allStatus.stream()
                    .filter(s -> s.getNextReviewDate() != null)
                    .filter(s -> s.getNextReviewDate().equals(date))
                    .filter(s -> s.getStatus() != ReviewStatus.SUSPENDED)
                    .count();

            ReviewStatsDTO.DailyForecast dayForecast = new ReviewStatsDTO.DailyForecast();
            dayForecast.setDate(date);
            dayForecast.setCount((int) count);
            forecast.add(dayForecast);
        }

        return forecast;
    }

    /**
     * 计算间隔天数分布
     */
    private Map<String, Integer> calculateIntervalDistribution(List<QuizReviewStatus> allStatus) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("0天", 0);
        distribution.put("1-7天", 0);
        distribution.put("8-30天", 0);
        distribution.put("31-90天", 0);
        distribution.put("90天以上", 0);

        for (QuizReviewStatus status : allStatus) {
            int interval = status.getCurrentInterval();
            if (interval == 0) {
                distribution.put("0天", distribution.get("0天") + 1);
            } else if (interval <= 7) {
                distribution.put("1-7天", distribution.get("1-7天") + 1);
            } else if (interval <= 30) {
                distribution.put("8-30天", distribution.get("8-30天") + 1);
            } else if (interval <= 90) {
                distribution.put("31-90天", distribution.get("31-90天") + 1);
            } else {
                distribution.put("90天以上", distribution.get("90天以上") + 1);
            }
        }

        return distribution;
    }

    /**
     * 计算简易度系数分布
     */
    private Map<String, Integer> calculateEaseDistribution(List<QuizReviewStatus> allStatus) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("1.3-1.9", 0);
        distribution.put("2.0-2.4", 0);
        distribution.put("2.5-2.9", 0);
        distribution.put("3.0-3.5", 0);
        distribution.put("3.6+", 0);

        for (QuizReviewStatus status : allStatus) {
            double ease = status.getEaseFactor() / 1000.0;
            if (ease < 2.0) {
                distribution.put("1.3-1.9", distribution.get("1.3-1.9") + 1);
            } else if (ease < 2.5) {
                distribution.put("2.0-2.4", distribution.get("2.0-2.4") + 1);
            } else if (ease < 3.0) {
                distribution.put("2.5-2.9", distribution.get("2.5-2.9") + 1);
            } else if (ease < 3.6) {
                distribution.put("3.0-3.5", distribution.get("3.0-3.5") + 1);
            } else {
                distribution.put("3.6+", distribution.get("3.6+") + 1);
            }
        }

        return distribution;
    }

    /**
     * 获取最近N天的复习历史记录
     * 注意：由于当前实现没有保存历史记录表，这里返回模拟数据
     * 实际项目中应该创建一个复习历史记录表
     */
    public List<ReviewStatsDTO.DailyReviewHistory> getReviewHistory(Long userId, int days) {
        List<ReviewStatsDTO.DailyReviewHistory> history = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 这里应该从复习历史记录表中查询
        // 暂时返回空数据，表示暂无历史记录
        for (int i = days - 1; i >= 0; i--) {
            ReviewStatsDTO.DailyReviewHistory day = new ReviewStatsDTO.DailyReviewHistory();
            day.setDate(today.minusDays(i));
            day.setReviewedCount(0);
            day.setCorrectCount(0);
            history.add(day);
        }

        return history;
    }

    /**
     * 获取用户的连续学习天数
     * 注意：需要实现打卡记录表
     */
    public int getStreakDays(Long userId) {
        // 这里应该从学习打卡记录表中查询连续天数
        // 暂时返回0
        return 0;
    }
}
