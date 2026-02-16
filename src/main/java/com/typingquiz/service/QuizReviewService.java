package com.typingquiz.service;

import com.typingquiz.dto.LearnResponseDTO;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.QuizReviewStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 测验复习服务
 * 提供复习相关的通用业务逻辑
 */
@Service
@Transactional
public class QuizReviewService {

    private static final Logger logger = LoggerFactory.getLogger(QuizReviewService.class);
    private static final java.time.ZoneId ZONE_ID = java.time.ZoneId.of("Asia/Shanghai");

    private final QuizReviewStatusRepository reviewStatusRepository;

    @Autowired
    public QuizReviewService(QuizReviewStatusRepository reviewStatusRepository) {
        this.reviewStatusRepository = reviewStatusRepository;
    }

    /**
     * 获取用户今日复习统计
     */
    public Map<String, Long> getTodayStats(Long userId) {
        Map<String, Long> stats = reviewStatusRepository.countByStatus(userId).stream()
                .collect(Collectors.toMap(
                        row -> ((ReviewStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));
        
        // 添加今日到期数量
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        Long dueToday = reviewStatusRepository.countDueToday(userId, now);
        stats.put("DUE_TODAY", dueToday);
        
        return stats;
    }

    /**
     * 获取今日复习统计数据
     */
    public Map<String, Object> getTodaySummary(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        
        // 学习中到期数量
        stats.put("LEARNING", reviewStatusRepository.countLearningDue(userId, now));
        
        // 复习中到期数量
        stats.put("REVIEW", reviewStatusRepository.countDueToday(userId, now) - (Long)stats.get("LEARNING"));
        
        // 新测验数量
        stats.put("NEW", reviewStatusRepository.countByUserIdAndStatus(userId, ReviewStatus.NEW));
        
        return stats;
    }

    /**
     * 获取今日剩余复习数量
     */
    public int getRemainingCountForToday(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        Long count = reviewStatusRepository.countDueToday(userId, now);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 搁置测验（延后复习）
     */
    public void buryQuiz(Long quizId, Long userId, int days) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));
        
        LocalDateTime buriedUntil = LocalDateTime.now(ZONE_ID).plusDays(days);
        status.setBuriedUntil(buriedUntil);
        
        reviewStatusRepository.save(status);
        logger.info("用户{}搁置测验{} {}天，直到{}", userId, quizId, days, buriedUntil);
    }

    /**
     * 暂停卡片
     */
    public void suspendCard(Long quizId, Long userId) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));
        
        status.setStatus(ReviewStatus.SUSPENDED);
        reviewStatusRepository.save(status);
        
        logger.info("用户{}暂停测验{}", userId, quizId);
    }

    /**
     * 恢复暂停的卡片
     */
    public void unsuspendCard(Long quizId, Long userId) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));
        
        if (status.getStatus() != ReviewStatus.SUSPENDED) {
            throw new RuntimeException("该测验未处于暂停状态");
        }
        
        // 恢复为NEW状态（未学习）
        status.setStatus(ReviewStatus.NEW);
        status.setBuriedUntil(null);
        reviewStatusRepository.save(status);
        
        logger.info("用户{}恢复测验{}", userId, quizId);
    }

    /**
     * 重置卡片（重新开始学习）
     */
    public void resetCard(Long quizId, Long userId) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));
        
        status.setStatus(ReviewStatus.NEW);
        status.setLearningStep(0);
        status.setIntervalDays(0);
        status.setEaseFactor(2500);
        status.setNextReviewDate(null);
        status.setBuriedUntil(null);
        status.setReviewCount(0);
        status.setLapseCount(0);
        
        reviewStatusRepository.save(status);
        
        logger.info("用户{}重置测验{}", userId, quizId);
    }

    /**
     * 获取用户所有复习状态
     */
    public List<QuizReviewStatus> getUserReviewStatuses(Long userId) {
        return reviewStatusRepository.findByUserId(userId);
    }

    /**
     * 获取今日到期复习列表（包含所有到期状态）
     */
    public List<QuizReviewStatus> getDueToday(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        return reviewStatusRepository.findDueToday(userId, now);
    }

    /**
     * 获取到期学习中测验（精确到时间）
     */
    public List<QuizReviewStatus> getLearningDue(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        return reviewStatusRepository.findLearningDue(userId, now);
    }

    /**
     * 获取到期复习测验（不包括学习中，精确到时间）
     */
    public List<QuizReviewStatus> getReviewDue(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        return reviewStatusRepository.findReviewDue(userId, now);
    }

    /**
     * 获取用户新测验列表
     */
    public List<QuizReviewStatus> getNewCards(Long userId) {
        return reviewStatusRepository.findNewCards(userId);
    }

    /**
     * 统计各状态数量（用于前端展示）
     */
    public Map<String, Long> getDueCounts(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        
        // 学习中到期数量（精确到时间）
        counts.put("learningDue", reviewStatusRepository.countLearningDue(userId, now));
        
        // 复习中到期数量
        counts.put("reviewDue", reviewStatusRepository.countDueToday(userId, now) - counts.get("learningDue"));
        
        // 新测验数量
        counts.put("newCards", reviewStatusRepository.countByUserIdAndStatus(userId, ReviewStatus.NEW));
        
        return counts;
    }

    /**
     * 获取测验复习状态
     */
    public QuizReviewStatus getQuizStatus(Long quizId, Long userId) {
        return reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseGet(() -> initializeQuizStatus(quizId, userId));
    }

    private static final int MAX_INTERVAL_DAYS = 365; // 最大间隔1年
    private static final int MIN_EASE_FACTOR = 1300;
    private static final int MAX_EASE_FACTOR = 3000;

    /**
     * 提交复习评级（复习阶段）
     */
    public LearnResponseDTO submitReviewRating(Long quizId, Long userId, Integer rating) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));
        
        // 复习阶段评级处理
        int intervalDays = status.getIntervalDays();
        int easeFactor = status.getEaseFactor();
        
        // 根据评级计算新的间隔天数和难度系数
        switch (rating) {
            case 1: // 重来
                intervalDays = 1;
                easeFactor = Math.max(MIN_EASE_FACTOR, easeFactor - 200);
                status.setStatus(ReviewStatus.RELEARNING);
                break;
            case 2: // 困难
                intervalDays = (int) Math.max(1, intervalDays * 1.2);
                easeFactor = Math.max(MIN_EASE_FACTOR, easeFactor - 150);
                break;
            case 3: // 良好
                if (intervalDays == 0) {
                    intervalDays = 1;
                } else {
                    // 修正：easeFactor 基准是 1000，所以除以 1000.0
                    intervalDays = (int) (intervalDays * (easeFactor / 1000.0));
                }
                break;
            case 4: // 简单
                if (intervalDays == 0) {
                    intervalDays = 4;
                } else {
                    // 修正：easeFactor 基准是 1000，并加上奖励系数 1.3
                    intervalDays = (int) (intervalDays * (easeFactor / 1000.0) * 1.3);
                }
                easeFactor = Math.min(MAX_EASE_FACTOR, easeFactor + 150);
                break;
        }
        
        // 限制最大间隔天数，防止爆炸
        intervalDays = Math.min(intervalDays, MAX_INTERVAL_DAYS);
        
        // 数据自愈：如果检测到已有间隔极度异常（脏数据），强制重置为最大允许值
        if (status.getIntervalDays() != null && status.getIntervalDays() > MAX_INTERVAL_DAYS * 10) {
            logger.warn("检测到异常间隔数据: quizId={}, interval={}, 强制修正", quizId, status.getIntervalDays());
            intervalDays = MAX_INTERVAL_DAYS;
        }
        
        // 更新状态
        status.setIntervalDays(intervalDays);
        status.setEaseFactor(easeFactor);
        status.setNextReviewDate(LocalDateTime.now().plusDays(intervalDays));
        status.setReviewCount(status.getReviewCount() + 1);
        
        // 如果完成复习，回到REVIEW状态
        if (status.getStatus() != ReviewStatus.RELEARNING) {
            status.setStatus(ReviewStatus.REVIEW);
        }
        
        reviewStatusRepository.save(status);
        
        LearnResponseDTO response = new LearnResponseDTO();
        response.setCompleted(true);
        response.setStatus(status.getStatus().name());
        response.setIntervalDays(intervalDays);
        response.setNextReviewDate(status.getNextReviewDate());
        
        return response;
    }

    /**
     * 初始化测验复习状态
     * 为新测验创建 NEW 状态的复习记录
     */
    public QuizReviewStatus initializeQuizStatus(Long quizId, Long userId) {
        Optional<QuizReviewStatus> existing = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        QuizReviewStatus status = new QuizReviewStatus();
        status.setQuizId(quizId);
        status.setUserId(userId);
        status.setStatus(ReviewStatus.NEW);
        status.setLearningStep(0);
        status.setIntervalDays(0);
        status.setEaseFactor(2500);
        status.setReviewCount(0);
        status.setLapseCount(0);
        
        reviewStatusRepository.save(status);
        logger.info("为用户{}初始化测验{}的复习状态", userId, quizId);
        return status;
    }
}
