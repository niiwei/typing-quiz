package com.typingquiz.service;

import com.typingquiz.entity.UserDailyActivity;
import com.typingquiz.repository.UserDailyActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * 用户每日活动统计服务
 * 用于聚合每日学习数据，支撑今日统计和热力图
 */
@Service
@Transactional
public class DailyActivityService {

    private static final Logger logger = LoggerFactory.getLogger(DailyActivityService.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final UserDailyActivityRepository dailyActivityRepository;

    @Autowired
    public DailyActivityService(UserDailyActivityRepository dailyActivityRepository) {
        this.dailyActivityRepository = dailyActivityRepository;
    }

    /**
     * 记录评级提交，更新今日统计
     *
     * @param userId    用户ID
     * @param rating    评级（1-4）
     * @param timeSpent 耗时（秒）
     * @param isNewCard 是否为新学卡片
     * @param status    当前状态（LEARNING/REVIEW/RELEARNING）
     */
    public void recordReviewRating(Long userId, int rating, int timeSpent, boolean isNewCard, String status) {
        LocalDate today = LocalDate.now(ZONE_ID);

        Optional<UserDailyActivity> existing = dailyActivityRepository
                .findByUserIdAndActivityDate(userId, today);

        UserDailyActivity activity;
        if (existing.isPresent()) {
            activity = existing.get();
        } else {
            // 检查昨天是否活跃，计算连续天数
            int streakDays = calculateStreakDays(userId);

            activity = new UserDailyActivity(userId, today);
            activity.setStreakDays(streakDays);
            activity.setIsActive(true);
        }

        // 更新统计
        activity.setReviewCount((activity.getReviewCount() == null ? 0 : activity.getReviewCount()) + 1);
        activity.setTotalTimeSeconds((activity.getTotalTimeSeconds() == null ? 0 : activity.getTotalTimeSeconds()) + timeSpent);

        // 新学卡片单独计数
        if (isNewCard) {
            activity.setNewLearnedCount((activity.getNewLearnedCount() == null ? 0 : activity.getNewLearnedCount()) + 1);
        }

        // 重学状态单独计数
        if ("RELEARNING".equals(status)) {
            activity.setRelearningCount((activity.getRelearningCount() == null ? 0 : activity.getRelearningCount()) + 1);
        }

        // 评级分布
        activity.incrementRatingCount(rating);

        // 标记活跃
        activity.setIsActive(true);

        dailyActivityRepository.save(activity);

        logger.info("更新今日统计: userId={}, rating={}, timeSpent={}, totalCards={}",
                userId, rating, timeSpent, activity.getTotalCards());
    }

    /**
     * 记录新测验创建
     */
    public void recordNewQuiz(Long userId) {
        // 新测验创建不需要立即更新今日统计
        // 统计在首次学习时记录
        logger.debug("记录新测验创建: userId={}", userId);
    }

    /**
     * 获取或创建今日活动记录
     */
    public UserDailyActivity getOrCreateTodayActivity(Long userId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        return dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .orElseGet(() -> {
                    UserDailyActivity newActivity = new UserDailyActivity(userId, today);
                    newActivity.setStreakDays(calculateStreakDays(userId));
                    return dailyActivityRepository.save(newActivity);
                });
    }

    /**
     * 获取今日统计
     */
    public UserDailyActivity getTodayStats(Long userId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        return dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .orElse(null);
    }

    /**
     * 计算连续天数
     * 如果昨天活跃，则连续天数+1；否则重置为1
     */
    private int calculateStreakDays(Long userId) {
        LocalDate yesterday = LocalDate.now(ZONE_ID).minusDays(1);

        Optional<UserDailyActivity> yesterdayActivity = dailyActivityRepository
                .findByUserIdAndActivityDate(userId, yesterday);

        if (yesterdayActivity.isPresent() && Boolean.TRUE.equals(yesterdayActivity.get().getIsActive())) {
            Integer yesterdayStreak = yesterdayActivity.get().getStreakDays();
            return (yesterdayStreak == null ? 0 : yesterdayStreak) + 1;
        }

        // 昨天未活跃，检查是否是今天首次活跃
        LocalDate today = LocalDate.now(ZONE_ID);
        Optional<UserDailyActivity> todayActivity = dailyActivityRepository
                .findByUserIdAndActivityDate(userId, today);

        // 如果今天已有记录且活跃，则保持当前连续天数
        if (todayActivity.isPresent() && Boolean.TRUE.equals(todayActivity.get().getIsActive())) {
            Integer todayStreak = todayActivity.get().getStreakDays();
            return todayStreak == null ? 1 : todayStreak;
        }

        return 1; // 新的开始
    }

    /**
     * 获取用户的总学习天数
     */
    public Long getTotalActiveDays(Long userId) {
        return dailyActivityRepository.countTotalActiveDaysByUserId(userId);
    }

    /**
     * 获取用户的当前连续天数
     */
    public Integer getCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        return dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .map(UserDailyActivity::getStreakDays)
                .orElse(0);
    }

    /**
     * 获取用户在指定日期范围内的活动记录（用于热力图）
     */
    public List<UserDailyActivity> getUserActivitiesInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return dailyActivityRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }
}
