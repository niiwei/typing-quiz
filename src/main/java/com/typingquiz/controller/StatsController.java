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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    public StatsController(DailyActivityService dailyActivityService, TrackService trackService) {
        this.dailyActivityService = dailyActivityService;
        this.trackService = trackService;
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
