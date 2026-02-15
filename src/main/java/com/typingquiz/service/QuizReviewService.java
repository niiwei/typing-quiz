package com.typingquiz.service;

import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.QuizReviewStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 测验复习服务
 * 提供复习相关的通用业务逻辑
 */
@Service
@Transactional
public class QuizReviewService {

    private static final Logger logger = LoggerFactory.getLogger(QuizReviewService.class);

    private final QuizReviewStatusRepository reviewStatusRepository;

    @Autowired
    public QuizReviewService(QuizReviewStatusRepository reviewStatusRepository) {
        this.reviewStatusRepository = reviewStatusRepository;
    }

    /**
     * 获取用户今日复习统计
     */
    public Map<String, Long> getTodayStats(Long userId) {
        LocalDate today = LocalDate.now();
        
        Map<String, Long> stats = reviewStatusRepository.countByStatus(userId).stream()
                .collect(Collectors.toMap(
                        row -> ((ReviewStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));
        
        // 添加今日到期数量
        Long dueToday = reviewStatusRepository.countDueToday(userId, today);
        stats.put("DUE_TODAY", dueToday);
        
        return stats;
    }

    /**
     * 获取今日剩余复习数量
     */
    public int getRemainingCountForToday(Long userId) {
        LocalDate today = LocalDate.now();
        Long count = reviewStatusRepository.countDueToday(userId, today);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 搁置卡片（推迟复习）
     * 
     * @param quizId 测验ID
     * @param userId 用户ID
     * @param days 推迟天数
     */
    public void buryCard(Long quizId, Long userId, int days) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));
        
        LocalDate buriedUntil = LocalDate.now().plusDays(days);
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
     * 获取今日到期复习列表
     */
    public List<QuizReviewStatus> getDueToday(Long userId) {
        return reviewStatusRepository.findDueToday(userId, LocalDate.now());
    }
}
