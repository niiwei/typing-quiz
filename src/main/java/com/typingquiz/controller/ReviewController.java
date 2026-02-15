package com.typingquiz.controller;

import com.typingquiz.dto.GroupReviewDTO;
import com.typingquiz.dto.QuizReviewItemDTO;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.entity.User;
import com.typingquiz.repository.QuizGroupRepository;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.service.QuizReviewService;
import com.typingquiz.service.ReviewStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 复习控制器
 * 提供分组维度的复习列表和统计API
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    // 学习阶段总步数
    private static final int TOTAL_LEARNING_STEPS = 3;

    private final QuizReviewService quizReviewService;
    private final QuizGroupRepository quizGroupRepository;
    private final QuizRepository quizRepository;
    private final ReviewStatsService reviewStatsService;

    @Autowired
    public ReviewController(QuizReviewService quizReviewService,
                           QuizGroupRepository quizGroupRepository,
                           QuizRepository quizRepository,
                           ReviewStatsService reviewStatsService) {
        this.quizReviewService = quizReviewService;
        this.quizGroupRepository = quizGroupRepository;
        this.quizRepository = quizRepository;
        this.reviewStatsService = reviewStatsService;
    }

    /**
     * 获取分组维度的复习统计
     * 用于首页显示各分组的新测验/复习数量角标
     */
    @GetMapping("/groups/summary")
    public ResponseEntity<List<GroupReviewDTO>> getGroupReviewSummary(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // 获取用户的所有分组
            List<QuizGroup> groups = quizGroupRepository.findByUserIdOrderByDisplayOrderAsc(userId);
            
            // 获取所有复习状态
            List<QuizReviewStatus> allStatuses = quizReviewService.getUserReviewStatuses(userId);
            
            // 按分组统计
            List<GroupReviewDTO> result = groups.stream().map(group -> {
                GroupReviewDTO dto = new GroupReviewDTO();
                dto.setGroupId(group.getId());
                dto.setGroupName(group.getName());
                
                // 统计该分组下的测验状态
                List<Long> groupQuizIds = group.getQuizzes().stream()
                        .map(Quiz::getId)
                        .collect(Collectors.toList());
                
                int newCount = 0;
                int learningCount = 0;
                int reviewCount = 0;
                int dueTodayCount = 0;
                LocalDate today = LocalDate.now();
                
                for (QuizReviewStatus status : allStatuses) {
                    if (groupQuizIds.contains(status.getQuizId())) {
                        switch (status.getStatus()) {
                            case NEW:
                                newCount++;
                                break;
                            case LEARNING:
                                learningCount++;
                                break;
                            case REVIEW:
                            case RELEARNING:
                                reviewCount++;
                                // 检查是否今日到期
                                if (status.getNextReviewDate() != null 
                                        && !status.getNextReviewDate().isAfter(today)
                                        && !status.isBuried()) {
                                    dueTodayCount++;
                                }
                                break;
                        }
                    }
                }
                
                dto.setNewCount(newCount);
                dto.setLearningCount(learningCount);
                dto.setReviewCount(reviewCount);
                dto.setDueTodayCount(dueTodayCount);
                dto.setExpandable(newCount + learningCount + reviewCount > 0);
                
                return dto;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取分组复习统计失败: userId={}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取指定分组下的测验复习列表
     * 用于展开分组后显示测验详情
     */
    @GetMapping("/groups/{groupId}/quizzes")
    public ResponseEntity<List<QuizReviewItemDTO>> getGroupQuizzes(
            @PathVariable Long groupId,
            @RequestParam(required = false) ReviewStatus status,
            HttpSession session) {
        
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // 验证分组归属
            QuizGroup group = quizGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("分组不存在"));
            if (!userId.equals(group.getUserId())) {
                return ResponseEntity.status(403).build();
            }

            // 获取分组下的所有测验
            List<Quiz> quizzes = group.getQuizzes();
            
            // 获取这些测验的复习状态
            List<QuizReviewItemDTO> result = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            for (Quiz quiz : quizzes) {
                QuizReviewStatus reviewStatus = quizReviewService.getUserReviewStatuses(userId)
                        .stream()
                        .filter(s -> s.getQuizId().equals(quiz.getId()))
                        .findFirst()
                        .orElse(null);
                
                if (reviewStatus == null) {
                    continue; // 跳过没有复习状态的测验
                }
                
                // 如果指定了状态过滤
                if (status != null && reviewStatus.getStatus() != status) {
                    continue;
                }
                
                QuizReviewItemDTO dto = convertToItemDTO(quiz, reviewStatus, today);
                result.add(dto);
            }
            
            // 排序：今日到期优先，然后是新测验，再是学习中，最后是其他
            result.sort((a, b) -> {
                if (a.isOverdue() && !b.isOverdue()) return -1;
                if (!a.isOverdue() && b.isOverdue()) return 1;
                return Integer.compare(getStatusPriority(a.getStatus()), getStatusPriority(b.getStatus()));
            });
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取分组测验列表失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取今日复习总览
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayOverview(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Map<String, Long> stats = quizReviewService.getTodayStats(userId);
            int dueToday = quizReviewService.getRemainingCountForToday(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("newCount", stats.getOrDefault("NEW", 0L));
            result.put("learningCount", stats.getOrDefault("LEARNING", 0L));
            result.put("reviewCount", stats.getOrDefault("REVIEW", 0L) + stats.getOrDefault("RELEARNING", 0L));
            result.put("suspendedCount", stats.getOrDefault("SUSPENDED", 0L));
            result.put("dueTodayCount", dueToday);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取今日复习总览失败: userId={}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取下一个待学习的测验
     * 优先顺序：逾期 > 新测验 > 学习中 > 待复习
     */
    @GetMapping("/next")
    public ResponseEntity<Map<String, Object>> getNextQuiz(
            @RequestParam(required = false) Long groupId,
            HttpSession session) {
        
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // 获取今日到期的测验
            List<QuizReviewStatus> dueToday = quizReviewService.getDueToday(userId);
            
            // 如果有指定分组，过滤
            if (groupId != null) {
                QuizGroup group = quizGroupRepository.findById(groupId).orElse(null);
                if (group != null) {
                    List<Long> groupQuizIds = group.getQuizzes().stream()
                            .map(Quiz::getId)
                            .collect(Collectors.toList());
                    dueToday = dueToday.stream()
                            .filter(s -> groupQuizIds.contains(s.getQuizId()))
                            .collect(Collectors.toList());
                }
            }
            
            // 如果没有今日到期的，找新测验
            if (dueToday.isEmpty()) {
                List<QuizReviewStatus> newCards = quizReviewService.getUserReviewStatuses(userId)
                        .stream()
                        .filter(s -> s.getStatus() == ReviewStatus.NEW)
                        .collect(Collectors.toList());
                
                if (groupId != null) {
                    QuizGroup group = quizGroupRepository.findById(groupId).orElse(null);
                    if (group != null) {
                        List<Long> groupQuizIds = group.getQuizzes().stream()
                                .map(Quiz::getId)
                                .collect(Collectors.toList());
                        newCards = newCards.stream()
                                .filter(s -> groupQuizIds.contains(s.getQuizId()))
                                .collect(Collectors.toList());
                    }
                }
                
                if (!newCards.isEmpty()) {
                    QuizReviewStatus next = newCards.get(0);
                    Quiz quiz = quizRepository.findById(next.getQuizId()).orElse(null);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("quizId", next.getQuizId());
                    result.put("quizTitle", quiz != null ? quiz.getTitle() : "未知");
                    result.put("status", next.getStatus());
                    result.put("type", "NEW");
                    
                    return ResponseEntity.ok(result);
                }
            } else {
                // 返回第一个今日到期的
                QuizReviewStatus next = dueToday.get(0);
                Quiz quiz = quizRepository.findById(next.getQuizId()).orElse(null);
                
                Map<String, Object> result = new HashMap<>();
                result.put("quizId", next.getQuizId());
                result.put("quizTitle", quiz != null ? quiz.getTitle() : "未知");
                result.put("status", next.getStatus());
                result.put("type", next.getStatus() == ReviewStatus.RELEARNING ? "RELEARNING" : "REVIEW");
                
                return ResponseEntity.ok(result);
            }
            
            // 没有可学习的
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("message", "今天没有待复习的测验");
            return ResponseEntity.ok(emptyResult);
        } catch (Exception e) {
            logger.error("获取下一个测验失败: userId={}, groupId={}", userId, groupId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private QuizReviewItemDTO convertToItemDTO(Quiz quiz, QuizReviewStatus status, LocalDate today) {
        QuizReviewItemDTO dto = new QuizReviewItemDTO();
        dto.setQuizId(quiz.getId());
        dto.setQuizTitle(quiz.getTitle());
        dto.setQuizDescription(quiz.getDescription());
        dto.setStatus(status.getStatus());
        dto.setStatusDisplay(status.getStatus().getDisplayName());
        dto.setLearningStep(status.getLearningStep());
        dto.setTotalLearningSteps(TOTAL_LEARNING_STEPS);
        dto.setNextReviewDate(status.getNextReviewDate());
        dto.setIntervalDays(status.getIntervalDays());
        dto.setEaseFactor(status.getEaseFactor());
        dto.setReviewCount(status.getReviewCount());
        dto.setLapseCount(status.getLapseCount());
        dto.setBuried(status.isBuried());
        dto.setBuriedUntil(status.getBuriedUntil());
        
        // 判断是否逾期
        if (status.getNextReviewDate() != null && status.getStatus() == ReviewStatus.REVIEW) {
            dto.setOverdue(status.getNextReviewDate().isBefore(today));
        }
        
        return dto;
    }

    /**
     * 获取复习统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getReviewStats(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            var stats = reviewStatsService.getUserStats(userId);
            stats.setReviewHistory(reviewStatsService.getReviewHistory(userId, 30));
            stats.setStreakDays(reviewStatsService.getStreakDays(userId));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取复习统计失败", e);
            return ResponseEntity.badRequest().body("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 搁置测验（推迟复习）
     */
    @PostMapping("/{quizId}/bury")
    public ResponseEntity<?> buryQuiz(@PathVariable Long quizId,
                                      @RequestBody(required = false) Map<String, Integer> body,
                                      HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            int days = body != null ? body.getOrDefault("days", 1) : 1;
            quizReviewService.buryCard(quizId, userId, days);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已搁置" + days + "天"
            ));
        } catch (Exception e) {
            logger.error("搁置测验失败", e);
            return ResponseEntity.badRequest().body("搁置失败: " + e.getMessage());
        }
    }

    /**
     * 暂停测验
     */
    @PostMapping("/{quizId}/suspend")
    public ResponseEntity<?> suspendQuiz(@PathVariable Long quizId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            quizReviewService.suspendCard(quizId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已暂停"
            ));
        } catch (Exception e) {
            logger.error("暂停测验失败", e);
            return ResponseEntity.badRequest().body("暂停失败: " + e.getMessage());
        }
    }

    /**
     * 恢复暂停的测验
     */
    @PostMapping("/{quizId}/unsuspend")
    public ResponseEntity<?> unsuspendQuiz(@PathVariable Long quizId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            quizReviewService.unsuspendCard(quizId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已恢复"
            ));
        } catch (Exception e) {
            logger.error("恢复测验失败", e);
            return ResponseEntity.badRequest().body("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 重置测验
     */
    @PostMapping("/{quizId}/reset")
    public ResponseEntity<?> resetQuiz(@PathVariable Long quizId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            quizReviewService.resetCard(quizId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已重置"
            ));
        } catch (Exception e) {
            logger.error("重置测验失败", e);
            return ResponseEntity.badRequest().body("重置失败: " + e.getMessage());
        }
    }

    /**
     * 按状态筛选测验
     */
    @GetMapping("/filter")
    public ResponseEntity<?> filterReviews(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long groupId,
            HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            List<QuizReviewStatus> results;

            if (status != null && !status.isEmpty()) {
                // 按状态筛选
                ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
                results = reviewStatusRepository.findByUserIdAndStatus(userId, reviewStatus);
            } else if (groupId != null) {
                // 按分组筛选
                results = reviewStatusRepository.findByUserId(userId).stream()
                    .filter(s -> {
                        Quiz quiz = quizRepository.findById(s.getQuizId()).orElse(null);
                        if (quiz == null) return false;
                        return quiz.getGroups().stream()
                            .anyMatch(g -> g.getId().equals(groupId));
                    })
                    .collect(Collectors.toList());
            } else {
                // 查询所有
                results = reviewStatusRepository.findByUserId(userId);
            }

            List<Map<String, Object>> filteredList = results.stream()
                .map(this::convertToSimpleMap)
                .collect(Collectors.toList());

            return ResponseEntity.ok(filteredList);
        } catch (Exception e) {
            logger.error("筛选测验失败", e);
            return ResponseEntity.badRequest().body("筛选失败: " + e.getMessage());
        }
    }

    /**
     * 获取搁置的卡片列表
     */
    @GetMapping("/buried")
    public ResponseEntity<?> getBuriedCards(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            List<QuizReviewStatus> buried = reviewStatusRepository.findBuriedCards(userId, LocalDate.now());
            List<Map<String, Object>> result = buried.stream()
                .map(this::convertToSimpleMap)
                .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取搁置卡片失败", e);
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    private Map<String, Object> convertToSimpleMap(QuizReviewStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("quizId", status.getQuizId());
        map.put("status", status.getStatus());
        map.put("intervalDays", status.getCurrentInterval());
        map.put("easeFactor", status.getEaseFactor());
        map.put("nextReviewDate", status.getNextReviewDate());
        map.put("reviewCount", status.getReviewCount());
        map.put("lapseCount", status.getLapseCount());
        map.put("buriedUntil", status.getBuriedUntil());
        return map;
    }

    private int getStatusPriority(ReviewStatus status) {
        switch (status) {
            case NEW: return 1;
            case LEARNING: return 2;
            case RELEARNING: return 3;
            case REVIEW: return 4;
            case SUSPENDED: return 5;
            default: return 6;
        }
    }

    private Long getCurrentUserId(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return user != null ? user.getId() : null;
    }
}
