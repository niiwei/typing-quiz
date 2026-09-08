package com.typingquiz.controller;

import com.typingquiz.entity.EventLog;
import com.typingquiz.entity.User;
import com.typingquiz.entity.UserDailyActivity;
import com.typingquiz.repository.EventLogRepository;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.repository.UserDailyActivityRepository;
import com.typingquiz.repository.UserRepository;
import com.typingquiz.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 * 提供站点统计数据的查询接口
 * 注意：这些接口需要管理员权限
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final EventLogRepository eventLogRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final UserDailyActivityRepository dailyActivityRepository;

    @Autowired
    public AdminController(EventLogRepository eventLogRepository,
                            UserRepository userRepository,
                            QuizRepository quizRepository,
                            UserDailyActivityRepository dailyActivityRepository) {
        this.eventLogRepository = eventLogRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.dailyActivityRepository = dailyActivityRepository;
    }

    /**
     * 获取站点统计数据
     * 包括：用户统计、PV统计、复习统计、设备分布等
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSiteStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "30") int days) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            LocalDateTime endTime = LocalDateTime.now(ZONE_ID);
            LocalDateTime startTime = endTime.minusDays(days);
            LocalDate today = LocalDate.now(ZONE_ID);

            // 1. 活跃用户统计（在时间段内有事件记录的用户）
            List<EventLog> allEvents = eventLogRepository.findByTimeRange(startTime, endTime);
            Set<Long> activeUserIds = allEvents.stream()
                    .map(EventLog::getUserId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            int activeUsers = activeUserIds.size();

            // 2. 总PV（页面访问事件）
            long totalPageViews = allEvents.stream()
                    .filter(e -> "page_view".equals(e.getEventType()))
                    .count();

            // 3. 总复习次数
            long totalReviews = allEvents.stream()
                    .filter(e -> "review_rating_submit".equals(e.getEventType()))
                    .count();

            // 4. 基础数据统计
            long totalQuizzes = quizRepository.count();
            long totalUsers = userRepository.count();
            
            // 今日新用户
            LocalDateTime todayStart = today.atStartOfDay();
            long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
            
            // 今日复习次数
            List<UserDailyActivity> todayActivities = dailyActivityRepository.findByActivityDate(today);
            int reviewsToday = todayActivities.stream()
                    .mapToInt(a -> a.getReviewCount() != null ? a.getReviewCount() : 0)
                    .sum();

            // 5. 用户活跃度分布
            Map<String, Integer> userDistribution = calculateUserDistribution(activeUserIds, days);

            // 6. 设备分布
            Map<String, Long> deviceDistribution = allEvents.stream()
                    .filter(e -> e.getEventData() != null && e.getEventData().contains("deviceType"))
                    .collect(Collectors.groupingBy(
                            e -> extractDeviceType(e.getEventData()),
                            Collectors.counting()
                    ));

            // 7. 每日趋势（按天聚合PV）
            List<Map<String, Object>> dailyTrend = calculateDailyTrend(allEvents, days);

            // 8. 热门页面（按path分组统计）
            Map<String, Long> pageViews = allEvents.stream()
                    .filter(e -> "page_view".equals(e.getEventType()))
                    .collect(Collectors.groupingBy(
                            e -> e.getPagePath() != null ? e.getPagePath() : "/unknown",
                            Collectors.counting()
                    ));
            List<Map<String, Object>> topPages = pageViews.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("path", e.getKey());
                        m.put("count", e.getValue());
                        return m;
                    })
                    .collect(Collectors.toList());

            // 9. 最近事件（最近20条）
            List<EventLog> recentEvents = allEvents.stream()
                    .sorted(Comparator.comparing(EventLog::getCreatedAt).reversed())
                    .limit(20)
                    .collect(Collectors.toList());
            List<Map<String, Object>> recentEventsData = recentEvents.stream()
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("userId", e.getUserId());
                        m.put("eventType", e.getEventType());
                        m.put("pagePath", e.getPagePath());
                        m.put("createdAt", e.getCreatedAt());
                        return m;
                    })
                    .collect(Collectors.toList());

            // 10. 获取所有用户信息
            List<User> allUsersList = userRepository.findAll();
            List<Map<String, Object>> userList = allUsersList.stream()
                    .map(u -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", u.getId());
                        m.put("username", u.getUsername());
                        m.put("email", u.getEmail());
                        m.put("createdAt", u.getCreatedAt());
                        return m;
                    })
                    .collect(Collectors.toList());

            // 组装结果
            Map<String, Object> result = new HashMap<>();
            result.put("activeUsers", activeUsers);
            result.put("totalPageViews", totalPageViews);
            result.put("totalReviews", totalReviews);
            result.put("totalQuizzes", totalQuizzes);
            result.put("totalUsers", totalUsers);
            result.put("newUsersToday", newUsersToday);
            result.put("reviewsToday", reviewsToday);
            result.put("userDistribution", userDistribution);
            result.put("deviceDistribution", deviceDistribution);
            result.put("dailyTrend", dailyTrend);
            result.put("topPages", topPages);
            result.put("recentEvents", recentEventsData);
            result.put("userList", userList);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取站点统计失败", e);
            return ResponseEntity.badRequest().body("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 计算用户活跃度分布
     */
    private Map<String, Integer> calculateUserDistribution(Set<Long> activeUserIds, int days) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("highActive", 0);  // 每天复习
        distribution.put("active", 0);     // 经常复习
        distribution.put("light", 0);      // 偶尔复习
        distribution.put("inactive", 0);   // 很少复习

        LocalDate today = LocalDate.now(ZONE_ID);

        for (Long userId : activeUserIds) {
            // 查询用户最近days天的活动天数
            LocalDate startDate = today.minusDays(days);
            List<UserDailyActivity> activities = dailyActivityRepository
                    .findByUserIdAndDateRange(userId, startDate, today);
            
            int activeDays = activities.size();
            double activityRate = days > 0 ? (double) activeDays / days : 0;

            if (activityRate >= 0.7) {
                distribution.put("highActive", distribution.get("highActive") + 1);
            } else if (activityRate >= 0.3) {
                distribution.put("active", distribution.get("active") + 1);
            } else if (activityRate >= 0.05) {
                distribution.put("light", distribution.get("light") + 1);
            } else {
                distribution.put("inactive", distribution.get("inactive") + 1);
            }
        }

        return distribution;
    }

    /**
     * 计算每日趋势
     */
    private List<Map<String, Object>> calculateDailyTrend(List<EventLog> allEvents, int days) {
        LocalDate today = LocalDate.now(ZONE_ID);
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            long count = allEvents.stream()
                    .filter(e -> e.getCreatedAt() != null 
                            && !e.getCreatedAt().isBefore(dayStart) 
                            && e.getCreatedAt().isBefore(dayEnd))
                    .count();

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", count);
            trend.add(dayData);
        }

        return trend;
    }

    /**
     * 从事件数据中提取设备类型
     */
    private String extractDeviceType(String eventData) {
        if (eventData == null) return "unknown";
        if (eventData.contains("\"deviceType\":\"mobile\"")) return "mobile";
        if (eventData.contains("\"deviceType\":\"tablet\"")) return "tablet";
        if (eventData.contains("\"deviceType\":\"desktop\"")) return "desktop";
        return "unknown";
    }

    /**
     * 获取所有注册用户列表
     * 常驻显示，不随日期筛选变化
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            List<User> allUsersList = userRepository.findAllByOrderByCreatedAtDesc();
            List<Map<String, Object>> userList = allUsersList.stream()
                    .map(u -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", u.getId());
                        m.put("username", u.getUsername());
                        m.put("email", u.getEmail());
                        m.put("createdAt", u.getCreatedAt());
                        return m;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("users", userList));
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return ResponseEntity.badRequest().body("获取用户列表失败: " + e.getMessage());
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
