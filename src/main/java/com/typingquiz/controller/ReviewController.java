package com.typingquiz.controller;

import com.typingquiz.dto.*;
import com.typingquiz.entity.*;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.service.*;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.QuizGroupRepository;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.repository.QuizReviewStatusRepository;
import com.typingquiz.service.LearningService;
import com.typingquiz.service.QuizReviewService;
import com.typingquiz.service.ReviewStatsService;
import com.typingquiz.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final QuizReviewStatusRepository reviewStatusRepository;
    private final LearningService learningService;

    @Autowired
    public ReviewController(QuizReviewService quizReviewService,
                           QuizGroupRepository quizGroupRepository,
                           QuizRepository quizRepository,
                           ReviewStatsService reviewStatsService,
                           QuizReviewStatusRepository reviewStatusRepository,
                           LearningService learningService) {
        this.quizReviewService = quizReviewService;
        this.quizGroupRepository = quizGroupRepository;
        this.quizRepository = quizRepository;
        this.reviewStatsService = reviewStatsService;
        this.reviewStatusRepository = reviewStatusRepository;
        this.learningService = learningService;
    }

    /**
     * 获取分组维度的复习统计
     * 用于首页显示各分组的新测验/复习数量角标
     */
    @GetMapping("/groups/summary")
    public ResponseEntity<?> getGroupReviewSummary(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录", "message", "请先登录"));
        }

        try {
            // 获取用户的所有分组
            List<QuizGroup> groups = quizGroupRepository.findByUserIdOrderByDisplayOrderAsc(userId);
            
            // 获取所有复习状态
            List<QuizReviewStatus> allStatuses = quizReviewService.getUserReviewStatuses(userId);
            
            // 统一使用北京时间
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            
            // 按分组统计
            List<GroupReviewDTO> result = groups.stream().map(group -> {
                GroupReviewDTO dto = new GroupReviewDTO();
                dto.setGroupId(group.getId());
                dto.setGroupName(group.getName());
                
                // 统计该分组下的测验状态
                List<Long> groupQuizIds = group.getQuizzes().stream()
                        .map(Quiz::getId)
                        .collect(Collectors.toList());
                
                // 使用 ReviewLabel 体系统计（与首页统计一致）
                int pendingLearnCount = 0;   // 待学习
                int pendingReviewCount = 0;  // 待复习
                int scheduledCount = 0;      // 未到期
                int suspendedCount = 0;      // 已暂停
                int dueTodayCount = 0;       // 今日到期总数
                
                for (QuizReviewStatus status : allStatuses) {
                    if (groupQuizIds.contains(status.getQuizId())) {
                        // 使用统一的准入逻辑进行计数
                        if (status.isUserAccessible()) {
                            ReviewLabel label = status.getLabel(now);
                            switch (label) {
                                case PENDING_LEARN:
                                    pendingLearnCount++;
                                    dueTodayCount++;
                                    break;
                                case PENDING_REVIEW:
                                    pendingReviewCount++;
                                    dueTodayCount++;
                                    break;
                                case SCHEDULED:
                                    scheduledCount++;
                                    break;
                                case SUSPENDED:
                                    suspendedCount++;
                                    break;
                            }
                        }
                    }
                }
                
                // 为了保持向后兼容，将新字段映射到旧字段
                dto.setNewCount(pendingLearnCount);        // 待学习
                dto.setReviewCount(pendingReviewCount);    // 待复习
                dto.setLearningCount(scheduledCount);      // 未到期/学习中
                dto.setRelearningCount(0);                 // 重学已合并到待学习
                dto.setDueTodayCount(dueTodayCount);
                dto.setExpandable(pendingLearnCount + pendingReviewCount > 0);
                
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
    public ResponseEntity<?> getGroupQuizzes(
            @PathVariable Long groupId,
            @RequestParam(required = false) ReviewStatus status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录", "message", "请先登录"));
        }

        try {
            // 验证分组归属
            QuizGroup group = quizGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("分组不存在"));
            if (!userId.equals(group.getUserId())) {
                return ResponseEntity.status(403).build();
            }

            // 获取分组下的所有测验ID
            List<Long> quizIds = group.getQuizzes().stream()
                    .map(Quiz::getId)
                    .collect(Collectors.toList());
            
            if (quizIds.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            // 使用 JOIN 查询一次性获取用户可访问的复习状态（避免N+1）
            List<QuizReviewStatus> allStatuses = reviewStatusRepository.findUserAccessibleStatuses(userId);
            
            // 过滤出分组中的测验
            Map<Long, QuizReviewStatus> statusMap = new HashMap<>();
            for (QuizReviewStatus rs : allStatuses) {
                if (quizIds.contains(rs.getQuizId())) {
                    statusMap.put(rs.getQuizId(), rs);
                }
            }
            
            // 构建结果
            List<QuizReviewItemDTO> result = new ArrayList<>();
            LocalDateTime today = LocalDateTime.now();
            
            for (Quiz quiz : group.getQuizzes()) {
                QuizReviewStatus reviewStatus = statusMap.get(quiz.getId());
                
                // 如果没有复习状态，自动创建 NEW 状态
                if (reviewStatus == null) {
                    reviewStatus = quizReviewService.initializeQuizStatus(quiz.getId(), userId);
                }
                
                // 如果指定了状态过滤
                if (status != null && reviewStatus.getStatus() != status) {
                    continue;
                }
                
                QuizReviewItemDTO dto = convertToItemDTO(quiz, reviewStatus, today);
                result.add(dto);
            }
            
            // 排序：与 getNextQuizForUser 保持一致
            // 优先级：LEARNING(1) > RELEARNING(2) > REVIEW(3) > NEW(4)
            // 同优先级按 nextReviewDate 排序（先到期优先）
            result.sort((a, b) -> {
                int pA = getStatusPriority(a.getStatus());
                int pB = getStatusPriority(b.getStatus());
                if (pA != pB) return Integer.compare(pA, pB);
                
                // 同优先级按下次复习时间排序
                if (a.getNextReviewDate() != null && b.getNextReviewDate() != null) {
                    return a.getNextReviewDate().compareTo(b.getNextReviewDate());
                }
                // null 视为最优先（立即到期）
                if (a.getNextReviewDate() == null && b.getNextReviewDate() != null) return -1;
                if (a.getNextReviewDate() != null && b.getNextReviewDate() == null) return 1;
                return 0;
            });
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取分组测验列表失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取全局待复习测验列表（用于全局复习模式）
     */
    @GetMapping("/quizzes")
    public ResponseEntity<?> getGlobalReviewQuizzes(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录", "message", "请先登录"));
        }

        try {
            // 使用 JOIN 查询一次性获取用户可访问的复习状态（避免N+1）
            List<QuizReviewStatus> allStatuses = reviewStatusRepository.findUserAccessibleStatuses(userId);
            
            // 批量获取所有测验ID对应的Quiz信息
            List<Long> quizIds = allStatuses.stream()
                    .map(QuizReviewStatus::getQuizId)
                    .collect(Collectors.toList());
            Map<Long, Quiz> quizMap = new HashMap<>();
            if (!quizIds.isEmpty()) {
                quizRepository.findAllById(quizIds).forEach(q -> quizMap.put(q.getId(), q));
            }
            
            // 只返回属于当前用户且有效分组的测验
            List<QuizReviewItemDTO> result = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (QuizReviewStatus status : allStatuses) {
                if (status == null || status.getStatus() == ReviewStatus.SUSPENDED) continue;
                
                // 从Map中获取测验信息（批量查询，无需逐个查询）
                Quiz quiz = quizMap.get(status.getQuizId());
                if (quiz == null) continue;
                
                // 检查测验是否属于当前用户
                if (quiz.getUserId() != null && !quiz.getUserId().equals(userId)) continue;
                
                // 检查测验是否属于有效分组（排除孤儿测验）
                if (quiz.getGroups() == null || quiz.getGroups().isEmpty()) continue;
                
                QuizReviewItemDTO dto = convertToItemDTO(quiz, status, now);
                result.add(dto);
            }
            
            // 排序：与 getNextQuizForUser 保持一致
            // 优先级：LEARNING(1) > RELEARNING(2) > REVIEW(3) > NEW(4)
            // 同优先级按 nextReviewDate 排序（先到期优先）
            result.sort((a, b) -> {
                int pA = getStatusPriority(a.getStatus());
                int pB = getStatusPriority(b.getStatus());
                if (pA != pB) return Integer.compare(pA, pB);
                
                // 同优先级按下次复习时间排序
                if (a.getNextReviewDate() != null && b.getNextReviewDate() != null) {
                    return a.getNextReviewDate().compareTo(b.getNextReviewDate());
                }
                // null 视为最优先（立即到期）
                if (a.getNextReviewDate() == null && b.getNextReviewDate() != null) return -1;
                if (a.getNextReviewDate() != null && b.getNextReviewDate() == null) return 1;
                return 0;
            });
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取全局复习列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取今日复习总览（使用 ReviewLabel 体系）
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayOverview(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录", "message", "请先登录"));
        }

        try {
            LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
            
            // 使用 JOIN 查询一次性获取用户可访问的复习状态（避免N+1）
            List<QuizReviewStatus> allStatuses = reviewStatusRepository.findUserAccessibleStatuses(userId);
            
            // 使用 ReviewLabel 体系统计
            int pendingLearnCount = 0;   // 待学习
            int pendingReviewCount = 0;  // 待复习
            int scheduledCount = 0;      // 未到期
            int suspendedCount = 0;      // 已暂停
            
            for (QuizReviewStatus status : allStatuses) {
                ReviewLabel label = status.getLabel(now);
                switch (label) {
                    case PENDING_LEARN:
                        pendingLearnCount++;
                        break;
                    case PENDING_REVIEW:
                        pendingReviewCount++;
                        break;
                    case SCHEDULED:
                        scheduledCount++;
                        break;
                    case SUSPENDED:
                        suspendedCount++;
                        break;
                }
            }
            
            int totalCount = pendingLearnCount + pendingReviewCount;
            
            logger.info("[今日复习统计] 用户{}: 总数={}, 待学习={}, 未到期={}, 待复习={}, 已暂停={}",
                userId, totalCount, pendingLearnCount, scheduledCount, pendingReviewCount, suspendedCount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("pendingLearnCount", pendingLearnCount);
            result.put("pendingReviewCount", pendingReviewCount);
            result.put("scheduledCount", scheduledCount);
            result.put("suspendedCount", suspendedCount);
            result.put("totalCount", totalCount);
            
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
            @RequestParam(required = false) Long currentQuizId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录", "message", "请先登录"));
        }

        try {
            // 使用 Anki 队列顺序获取下一个测验
            Map<String, Object> nextQuiz = getNextQuizForUser(userId, groupId, currentQuizId);
            
            // 诊断逻辑：如果用户请求了诊断信息（可以通过 query 参数触发）
            // 或者我们可以默认在 nextQuiz 为空时，返回该分组内第一个未准入测验的状态
            if (nextQuiz == null && groupId != null) {
                Map<String, Object> diagResult = new HashMap<>();
                diagResult.put("message", "该分组暂无待复习测验");
                
                // 尝试找出一个“准入失败”的测验作为诊断样例
                QuizGroup group = quizGroupRepository.findById(groupId).orElse(null);
                if (group != null) {
                    List<Long> ids = group.getQuizzes().stream().map(Quiz::getId).collect(Collectors.toList());
                    List<QuizReviewStatus> all = quizReviewService.getUserReviewStatuses(userId);
                    QuizReviewStatus sample = all.stream().filter(s -> ids.contains(s.getQuizId())).findFirst().orElse(null);
                    if (sample != null) {
                        Map<String, Object> debug = new HashMap<>();
                        debug.put("quizId", sample.getQuizId());
                        debug.put("status", sample.getStatus());
                        debug.put("nextReviewDate", sample.getNextReviewDate());
                        debug.put("isBuried", sample.isBuried());
                        debug.put("intervalDays", sample.getIntervalDays());
                        debug.put("serverTime", LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
                        diagResult.put("debugInfo", debug);
                    }
                }
                return ResponseEntity.ok(diagResult);
            }
            
            if (nextQuiz != null) {
                // 注入当前的服务器诊断时间
                nextQuiz.put("debugServerTime", LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).toString());
            }
            
            return ResponseEntity.ok(nextQuiz);
        } catch (Exception e) {
            logger.error("获取下一个测验失败: userId={}, groupId={}", userId, groupId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private QuizReviewItemDTO convertToItemDTO(Quiz quiz, QuizReviewStatus status, LocalDateTime now) {
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
        
        // 使用统一的标签判断逻辑（包含时区处理）
        ReviewLabel label = status.getLabel(now);
        dto.setLabel(label.name());
        dto.setLabelDisplay(label.getDisplayName());
        dto.setDue(label.isDueToday());
        dto.setUserId(status.getUserId());  // 设置userId用于诊断
        
        // 增加细化标签：区分新测验和待学习
        String detailedLabel = calculateDetailedLabel(status, label, now);
        dto.setDetailedLabel(detailedLabel);
        
        return dto;
    }
    
    /**
     * 计算细化标签，区分新测验和待学习
     */
    private String calculateDetailedLabel(QuizReviewStatus status, ReviewLabel label, LocalDateTime now) {
        if (label == ReviewLabel.SUSPENDED) {
            return "SUSPENDED";
        }
        if (label == ReviewLabel.SCHEDULED) {
            return "SCHEDULED";
        }
        if (label == ReviewLabel.PENDING_REVIEW) {
            return "PENDING_REVIEW";
        }
        // PENDING_LEARN 需要进一步区分
        if (status.getStatus() == ReviewStatus.NEW) {
            return "NEW";  // 新测验
        } else if (status.getStatus() == ReviewStatus.LEARNING) {
            return "PENDING_LEARN";  // 学习中且已到期
        } else if (status.getStatus() == ReviewStatus.RELEARNING) {
            return "PENDING_RELEARN";  // 重学中且已到期
        }
        return "PENDING_LEARN";
    }

    /**
     * 获取复习统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getReviewStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
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
     * 提交学习评级（新测验/学习阶段/重新学习阶段）
     */
    @PostMapping("/{quizId}/learn")
    public ResponseEntity<?> submitLearnRating(
            @PathVariable Long quizId,
            @RequestBody Map<String, Integer> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        Integer rating = request.get("rating");
        if (rating == null || rating < 1 || rating > 4) {
            return ResponseEntity.badRequest().body("评级必须在1-4之间");
        }

        try {
            LearnResponseDTO response = learningService.submitLearningRating(quizId, userId, rating);
            
            // 获取分组ID（如果有）
            Integer groupIdInt = request.get("groupId");
            Long groupId = groupIdInt != null ? groupIdInt.longValue() : null;
            
            // 获取下一个测验（无论当前是否完成）
            // 延迟 500ms，确保数据库事务已完全提交且状态已刷新
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            Map<String, Object> nextQuiz = getNextQuizForUser(userId, groupId, quizId);
            if (nextQuiz != null && nextQuiz.containsKey("quizId")) {
                response.setNextQuizId((Long) nextQuiz.get("quizId"));
                response.setNextQuizTitle((String) nextQuiz.get("quizTitle"));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("提交学习评级失败: quizId={}, userId={}, rating={}", quizId, userId, rating, e);
            return ResponseEntity.badRequest().body("提交失败: " + e.getMessage());
        }
    }

    /**
     * 提交复习评级（复习阶段）
     */
    @PostMapping("/{quizId}/review")
    public ResponseEntity<?> submitReviewRating(
            @PathVariable Long quizId,
            @RequestBody Map<String, Integer> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        Integer rating = request.get("rating");
        if (rating == null || rating < 1 || rating > 4) {
            return ResponseEntity.badRequest().body("评级必须在1-4之间");
        }

        try {
            // 处理复习评级
            LearnResponseDTO response = quizReviewService.submitReviewRating(quizId, userId, rating);
            
            // 获取分组ID（如果有）
            Integer groupIdInt = request.get("groupId");
            Long groupId = groupIdInt != null ? groupIdInt.longValue() : null;
            
            // 获取下一个测验（无论当前是否完成）
            // 延迟 500ms，确保数据库事务已完全提交且状态已刷新
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            Map<String, Object> nextQuiz = getNextQuizForUser(userId, groupId, quizId);
            if (nextQuiz != null && nextQuiz.containsKey("quizId")) {
                response.setNextQuizId((Long) nextQuiz.get("quizId"));
                response.setNextQuizTitle((String) nextQuiz.get("quizTitle"));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("提交复习评级失败: quizId={}, userId={}, rating={}", quizId, userId, rating, e);
            return ResponseEntity.badRequest().body("提交失败: " + e.getMessage());
        }
    }

    /**
     * 重置测验状态
     */
    @PostMapping("/{quizId}/reset")
    public ResponseEntity<?> resetQuizStatus(
            @PathVariable Long quizId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            quizReviewService.resetCard(quizId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "已重置测验状态"));
        } catch (Exception e) {
            logger.error("重置测验状态失败", e);
            return ResponseEntity.badRequest().body("重置失败: " + e.getMessage());
        }
    }

    /**
     * 获取测验状态
     */
    @GetMapping("/{quizId}/status")
    public ResponseEntity<?> getQuizStatus(
            @PathVariable Long quizId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        try {
            QuizReviewStatus status = quizReviewService.getQuizStatus(quizId, userId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取测验状态失败", e);
            return ResponseEntity.badRequest().body("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户下一个待复习测验（遵循 Anki 队列顺序）
     * 优先级：1.学习/重学中 2.待复习 3.新测验
     */
    private Map<String, Object> getNextQuizForUser(Long userId, Long groupId, Long currentQuizId) {
        try {
            logger.info("[抽取检查] 开始为用户 {} 获取下一个测验, 分组: {}, 排除当前测验: {}", userId, groupId, currentQuizId);
            // 获取用户所有复习状态记录
            List<QuizReviewStatus> allStatuses = quizReviewService.getUserReviewStatuses(userId);
            
            // 如果指定了分组，先过滤出该分组的 ID 列表
            List<Long> groupQuizIds = null;
            if (groupId != null) {
                QuizGroup group = quizGroupRepository.findById(groupId).orElse(null);
                if (group != null) {
                    groupQuizIds = group.getQuizzes().stream()
                            .map(Quiz::getId)
                            .collect(Collectors.toList());
                }
            }
            
            final List<Long> targetQuizIds = groupQuizIds;
            
            // 准入过滤：只保留 isUserAccessible() 为 true 且不是当前测验的记录
            List<QuizReviewStatus> accessibleStatuses = allStatuses.stream()
                    .filter(s -> targetQuizIds == null || targetQuizIds.contains(s.getQuizId()))
                    .filter(s -> currentQuizId == null || !s.getQuizId().equals(currentQuizId))
                    .filter(QuizReviewStatus::isUserAccessible)
                    .collect(Collectors.toList());
            
            if (accessibleStatuses.isEmpty()) {
                logger.info("[抽取检查] 无可用测验（已过滤排除项）");
                return null;
            }
            
            // 排序：按照优先级 (1.学习/重学中 2.待复习 3.新测验)
            accessibleStatuses.sort((a, b) -> {
                int pA = getStatusPriority(a.getStatus());
                int pB = getStatusPriority(b.getStatus());
                if (pA != pB) return Integer.compare(pA, pB);
                
                // 同优先级下，按下次复习时间排序（先到期的优先）
                if (a.getNextReviewDate() != null && b.getNextReviewDate() != null) {
                    return a.getNextReviewDate().compareTo(b.getNextReviewDate());
                }
                return 0;
            });
            
            // 遍历排序后的列表，找到第一个实际存在的测验
            for (QuizReviewStatus status : accessibleStatuses) {
                Long quizId = status.getQuizId();
                // 验证测验是否存在且属于当前用户
                Map<String, Object> quizMap = convertToNextQuizMap(status, userId);
                if (quizMap != null) {
                    logger.info("[抽取检查] 最终选中测验: {}", quizId);
                    return quizMap;
                } else {
                    logger.warn("[抽取检查] 测验 {} 不存在，跳过", quizId);
                }
            }
            
            logger.info("[抽取检查] 所有候选测验都不存在");
            return null;
        } catch (Exception e) {
            logger.error("获取下一个测验失败", e);
            return null;
        }
    }

    private int getStatusPriority(ReviewStatus status) {
        switch (status) {
            case LEARNING: return 1;
            case RELEARNING: return 2;
            case REVIEW: return 3;
            case NEW: return 4;
            case SUSPENDED: return 5;
            default: return 6;
        }
    }

    private Map<String, Object> convertToNextQuizMap(QuizReviewStatus status, Long userId) {
        Quiz quiz = quizRepository.findById(status.getQuizId()).orElse(null);
        // 验证测验属于当前用户
        if (quiz == null || !userId.equals(quiz.getUserId())) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("quizId", status.getQuizId());
        result.put("quizTitle", quiz.getTitle());
        result.put("status", status.getStatus());
        return result;
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
