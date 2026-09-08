package com.typingquiz.controller;

import com.typingquiz.dto.TodayStatsDTO;
import com.typingquiz.entity.UserDailyActivity;
import com.typingquiz.service.DailyActivityService;
import com.typingquiz.service.TrackService;
import com.typingquiz.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.QuizReviewStatusRepository;
import java.util.stream.Collectors;

/**
 * 统计数据控制器
 * 提供今日统计、热力图等数据查询API
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final DailyActivityService dailyActivityService;
    private final TrackService trackService;
    private final QuizReviewStatusRepository reviewStatusRepository;

    @Autowired
    public StatsController(DailyActivityService dailyActivityService, 
                           TrackService trackService,
                           QuizReviewStatusRepository reviewStatusRepository) {
        this.dailyActivityService = dailyActivityService;
        this.trackService = trackService;
        this.reviewStatusRepository = reviewStatusRepository;
    }

    /**
     * 获取今日学习统计（对应Anki"今天"页面）
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodayStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            UserDailyActivity activity = dailyActivityService.getTodayStats(userId);

            if (activity == null) {
                // 今日无数据，返回空统计
                return ResponseEntity.ok(TodayStatsDTO.empty());
            }

            TodayStatsDTO dto = convertToTodayStatsDTO(activity);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("获取今日统计失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取热力图数据（对应Anki"日程表"）
     */
    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmapData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "365") int days) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            LocalDate endDate = LocalDate.now(ZONE_ID);
            LocalDate startDate = endDate.minusDays(days);

            List<UserDailyActivity> activities = dailyActivityService
                    .getUserActivitiesInRange(userId, startDate, endDate);

            List<Map<String, Object>> heatmapData = activities.stream()
                    .map(a -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("date", a.getActivityDate().toString());
                        item.put("count", a.getTotalCards());
                        item.put("level", a.calculateActivityLevel());
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("year", endDate.getYear());
            result.put("data", heatmapData);
            result.put("totalDays", activities.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取热力图数据失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取连续天数
     */
    @GetMapping("/streak")
    public ResponseEntity<?> getStreakDays(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            Integer streakDays = dailyActivityService.getCurrentStreak(userId);
            Long totalActiveDays = dailyActivityService.getTotalActiveDays(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("currentStreak", streakDays);
            result.put("totalActiveDays", totalActiveDays);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取连续天数失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 转换实体为DTO
     */
    private TodayStatsDTO convertToTodayStatsDTO(UserDailyActivity activity) {
        TodayStatsDTO dto = new TodayStatsDTO();

        int totalCards = activity.getTotalCards();
        int totalTimeSeconds = activity.getTotalTimeSeconds() != null ? activity.getTotalTimeSeconds() : 0;

        dto.setTotalCards(totalCards);
        dto.setTotalTimeMinutes(totalTimeSeconds / 60.0);
        dto.setAvgTimePerCard(totalCards > 0 ? totalTimeSeconds / totalCards : 0);

        // 评级分布
        int againCount = activity.getAgainCount() != null ? activity.getAgainCount() : 0;
        int hardCount = activity.getHardCount() != null ? activity.getHardCount() : 0;
        int goodCount = activity.getGoodCount() != null ? activity.getGoodCount() : 0;
        int easyCount = activity.getEasyCount() != null ? activity.getEasyCount() : 0;

        dto.setAgainCount(againCount);
        dto.setAgainPercent(totalCards > 0 ? (againCount * 100.0 / totalCards) : 0);

        dto.setNewCards(activity.getNewLearnedCount() != null ? activity.getNewLearnedCount() : 0);
        dto.setReviewCards(activity.getReviewCount() != null ? activity.getReviewCount() : 0);
        dto.setRelearningCards(activity.getRelearningCount() != null ? activity.getRelearningCount() : 0);

        dto.setStreakDays(activity.getStreakDays() != null ? activity.getStreakDays() : 0);

        return dto;
    }

    /**
     * 获取复习历史记录（对应Anki"复习次数"图表）
     * 返回过去N天每天的复习数量
     */
    @GetMapping("/history")
    public ResponseEntity<?> getReviewHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "30") int days) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            LocalDate endDate = LocalDate.now(ZONE_ID);
            LocalDate startDate = endDate.minusDays(days);

            // 从每日活动表中获取历史记录
            List<UserDailyActivity> activities = dailyActivityService
                    .getUserActivitiesInRange(userId, startDate, endDate);

            // 构建日期到复习数量的映射
            Map<LocalDate, UserDailyActivity> activityMap = activities.stream()
                    .collect(Collectors.toMap(UserDailyActivity::getActivityDate, a -> a));

            // 生成完整的历史数据（包含无记录的日期）
            List<Map<String, Object>> history = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                UserDailyActivity activity = activityMap.get(date);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.toString());

                if (activity != null) {
                    dayData.put("reviewedCount", activity.getTotalCards());
                    dayData.put("newCards", activity.getNewLearnedCount() != null ? activity.getNewLearnedCount() : 0);
                    dayData.put("reviewCards", activity.getReviewCount() != null ? activity.getReviewCount() : 0);
                    dayData.put("relearningCards", activity.getRelearningCount() != null ? activity.getRelearningCount() : 0);
                    dayData.put("timeSpent", activity.getTotalTimeSeconds() != null ? activity.getTotalTimeSeconds() : 0);
                    dayData.put("againCount", activity.getAgainCount() != null ? activity.getAgainCount() : 0);
                } else {
                    dayData.put("reviewedCount", 0);
                    dayData.put("newCards", 0);
                    dayData.put("reviewCards", 0);
                    dayData.put("relearningCards", 0);
                    dayData.put("timeSpent", 0);
                    dayData.put("againCount", 0);
                }

                history.add(dayData);
            }

            // 计算汇总数据
            int totalReviewed = activities.stream()
                    .mapToInt(UserDailyActivity::getTotalCards)
                    .sum();
            int activeDays = (int) activities.stream()
                    .filter(a -> a.getTotalCards() > 0)
                    .count();
            int totalTime = activities.stream()
                    .mapToInt(a -> a.getTotalTimeSeconds() != null ? a.getTotalTimeSeconds() : 0)
                    .sum();

            Map<String, Object> result = new HashMap<>();
            result.put("history", history);
            result.put("totalReviewed", totalReviewed);
            result.put("activeDays", activeDays);
            result.put("averagePerDay", activeDays > 0 ? totalReviewed / activeDays : 0);
            result.put("totalTimeSeconds", totalTime);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取复习历史失败: userId={}, days={}", userId, days, e);
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取测验状态分布（对应Anki"卡片数量"饼图）
     */
    @GetMapping("/distribution")
    public ResponseEntity<?> getCardDistribution(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            // 从复习状态表中统计各状态数量
            var allStatus = reviewStatusRepository.findByUserId(userId);

            // 按状态分组统计
            Map<String, Long> statusCounts = allStatus.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getStatus().name(),
                            Collectors.counting()
                    ));

            // 计算总数
            long total = allStatus.size();

            // 构建分布数据（使用项目自有的状态名称）
            Map<String, Object> distribution = new HashMap<>();
            distribution.put("新测验", statusCounts.getOrDefault("NEW", 0L));
            distribution.put("学习中", statusCounts.getOrDefault("LEARNING", 0L));
            distribution.put("待复习", statusCounts.getOrDefault("REVIEW", 0L));
            distribution.put("重学中", statusCounts.getOrDefault("RELEARNING", 0L));
            distribution.put("已暂停", statusCounts.getOrDefault("SUSPENDED", 0L));

            // 计算百分比
            Map<String, Object> percentages = new HashMap<>();
            for (Map.Entry<String, Object> entry : distribution.entrySet()) {
                long count = (Long) entry.getValue();
                percentages.put(entry.getKey(), total > 0 ? Math.round(count * 100.0 / total * 100) / 100.0 : 0);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("counts", distribution);
            result.put("percentages", percentages);
            result.put("total", total);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取测验分布失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    private Long getCurrentUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!JwtUtil.validateToken(token)) {
            return null;
        }
        return JwtUtil.getUserIdFromToken(token);
    }
}
