package com.typingquiz.service;

import com.typingquiz.dto.ReviewResponseDTO;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.QuizReviewStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 复习阶段服务
 * 实现SM-2间隔重复算法
 */
@Service
@Transactional
public class ReviewStageService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewStageService.class);

    // 重学间隔（分钟）
    private static final List<Integer> RELEARNING_STEPS_MINUTES = Arrays.asList(10, 60);

    // 算法参数
    private static final int DEFAULT_EASE = 2500;
    private static final int MIN_EASE = 1300;
    private static final int MAX_EASE = 5000;
    private static final int EASE_DECREASE_AGAIN = 200;
    private static final int EASE_DECREASE_HARD = 150;
    private static final int EASE_INCREASE_EASY = 150;
    private static final double HARD_MULTIPLIER = 1.2;
    private static final double EASY_MULTIPLIER = 1.35;

    // 毕业间隔（学习阶段完成后首次复习间隔）
    private static final int GRADUATE_INTERVAL = 1;
    // 简单间隔（简单评级后首次复习间隔）
    private static final int EASY_INTERVAL = 4;
    // 重学后最小间隔
    private static final int MIN_RELEARN_INTERVAL = 1;
    // 重来后的新间隔乘数
    private static final double AGAIN_MULTIPLIER = 0.0;

    private final QuizReviewStatusRepository reviewStatusRepository;

    @Autowired
    public ReviewStageService(QuizReviewStatusRepository reviewStatusRepository) {
        this.reviewStatusRepository = reviewStatusRepository;
    }

    private static final java.time.ZoneId ZONE_ID = java.time.ZoneId.of("Asia/Shanghai");

    /**
     * 提交复习评级
     */
    public ReviewResponseDTO submitReviewRating(Long quizId, Long userId, int rating) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));

        // 验证状态是否可复习
        if (status.getStatus() != ReviewStatus.REVIEW && status.getStatus() != ReviewStatus.RELEARNING) {
            throw new RuntimeException("该测验不在复习阶段，当前状态：" + status.getStatus());
        }

        ReviewResponseDTO response = new ReviewResponseDTO();
        response.setQuizId(quizId);
        response.setRating(rating);

        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        int oldInterval = status.getIntervalDays();
        int oldEase = status.getEaseFactor();
        int newInterval;
        int newEase = oldEase;
        ReviewStatus newStatus = status.getStatus();

        switch (rating) {
            case 1: // 重来 - 进入重学阶段
                newEase = Math.max(MIN_EASE, oldEase - EASE_DECREASE_AGAIN);
                newStatus = ReviewStatus.RELEARNING;
                status.setLearningStep(0);
                status.setIntervalDays(MIN_RELEARN_INTERVAL);
                // 设置重学第一步的间隔
                int firstStep = RELEARNING_STEPS_MINUTES.get(0);
                status.setNextReviewDate(now.plusMinutes(firstStep));
                
                response.setNewStatus(newStatus);
                response.setIntervalDays(MIN_RELEARN_INTERVAL);
                response.setNextIntervalMinutes(firstStep);
                response.setMessage("需要重新学习，" + formatMinutes(firstStep) + "后继续");
                response.setCompleted(false);
                break;

            case 2: // 困难
                newEase = Math.max(MIN_EASE, oldEase - EASE_DECREASE_HARD);
                // 困难：间隔 = 原间隔 * 1.2
                newInterval = (int) (oldInterval * HARD_MULTIPLIER);
                newInterval = applyFuzz(newInterval);
                
                status.setIntervalDays(newInterval);
                status.setEaseFactor(newEase);
                status.setNextReviewDate(now.plusDays(newInterval));
                
                response.setIntervalDays(newInterval);
                response.setMessage("记住但较困难，" + newInterval + "天后复习");
                response.setCompleted(true);
                break;

            case 3: // 良好
                // 良好：间隔 = 原间隔 * 简易度系数
                newInterval = (int) (oldInterval * (double) oldEase / 1000.0);
                newInterval = applyFuzz(newInterval);
                
                status.setIntervalDays(newInterval);
                status.setNextReviewDate(now.plusDays(newInterval));
                
                response.setIntervalDays(newInterval);
                response.setMessage("记住，" + newInterval + "天后复习");
                response.setCompleted(true);
                break;

            case 4: // 简单
                newEase = Math.min(MAX_EASE, oldEase + EASE_INCREASE_EASY);
                // 简单：间隔 = 原间隔 * 简易度系数 * 1.35
                newInterval = (int) (oldInterval * (double) oldEase / 1000.0 * EASY_MULTIPLIER);
                newInterval = applyFuzz(newInterval);
                
                status.setIntervalDays(newInterval);
                status.setEaseFactor(newEase);
                status.setNextReviewDate(now.plusDays(newInterval));
                
                response.setIntervalDays(newInterval);
                response.setMessage("记住且轻松，" + newInterval + "天后复习");
                response.setCompleted(true);
                break;

            default:
                throw new IllegalArgumentException("无效的评级：" + rating);
        }

        // 更新统计
        status.setReviewCount(status.getReviewCount() + 1);
        status.setLastReviewDate(now);
        if (rating == 1) {
            status.setLapseCount(status.getLapseCount() + 1);
        }
        status.setStatus(newStatus);

        reviewStatusRepository.save(status);

        logger.info("用户{}完成测验{}的复习评级{}, 新间隔{}天, 新简易度{}",
                userId, quizId, rating, status.getIntervalDays(), status.getEaseFactor());

        return response;
    }

    /**
     * 提交重学阶段评级
     */
    public ReviewResponseDTO submitRelearningRating(Long quizId, Long userId, int rating) {
        QuizReviewStatus status = reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到复习状态"));

        if (status.getStatus() != ReviewStatus.RELEARNING) {
            throw new RuntimeException("该测验不在重学阶段");
        }

        ReviewResponseDTO response = new ReviewResponseDTO();
        response.setQuizId(quizId);
        response.setRating(rating);

        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        int currentStep = status.getLearningStep();
        int totalSteps = RELEARNING_STEPS_MINUTES.size();

        switch (rating) {
            case 1: // 重来 - 返回重学第一步
                status.setLearningStep(0);
                int firstStep = RELEARNING_STEPS_MINUTES.get(0);
                status.setNextReviewDate(now.plusMinutes(firstStep));
                
                response.setCompleted(false);
                response.setMessage("再来一次，" + formatMinutes(firstStep) + "后继续");
                response.setNextIntervalMinutes(firstStep);
                break;

            case 2: // 困难 - 重复当前步骤
                int hardDelay = calculateRelearnHardDelay(currentStep);
                status.setNextReviewDate(now.plusMinutes(hardDelay));
                response.setCompleted(false);
                response.setMessage("再复习一下，" + formatMinutes(hardDelay) + "后继续");
                response.setNextIntervalMinutes(hardDelay);
                break;

            case 3: // 良好 - 进入下一步或重新毕业
            case 4: // 简单 - 直接毕业
                if (rating == 4 || currentStep >= totalSteps - 1) {
                    // 完成重学，重新毕业
                    graduateFromRelearning(status, now);
                    response.setCompleted(true);
                    response.setNewStatus(ReviewStatus.REVIEW);
                    response.setIntervalDays(status.getIntervalDays());
                    response.setMessage("重学完成！" + status.getIntervalDays() + "天后复习");
                } else {
                    // 进入下一步
                    int nextStep = currentStep + 1;
                    status.setLearningStep(nextStep);
                    int nextDelay = RELEARNING_STEPS_MINUTES.get(nextStep);
                    status.setNextReviewDate(now.plusMinutes(nextDelay));
                    response.setCompleted(false);
                    response.setMessage("很好，" + formatMinutes(nextDelay) + "后继续");
                    response.setNextIntervalMinutes(nextDelay);
                }
                break;

            default:
                throw new IllegalArgumentException("无效的评级：" + rating);
        }

        status.setReviewCount(status.getReviewCount() + 1);
        status.setLastReviewDate(now);
        reviewStatusRepository.save(status);

        return response;
    }

    /**
     * 从重学阶段毕业
     */
    private void graduateFromRelearning(QuizReviewStatus status, LocalDateTime now) {
        int oldInterval = status.getIntervalDays();
        
        // 重学毕业后间隔 = max(最小间隔, 原间隔 * 0.0) = 1天
        // 或根据Anki规则，可以设置为原间隔 * 0.5 等
        int newInterval = MIN_RELEARN_INTERVAL;
        
        status.setStatus(ReviewStatus.REVIEW);
        status.setLearningStep(0);
        status.setIntervalDays(newInterval);
        status.setNextReviewDate(now.plusDays(newInterval));
    }

    /**
     * 计算重学阶段困难按钮的延迟
     */
    private int calculateRelearnHardDelay(int currentStep) {
        if (currentStep == 0) {
            return Math.min(RELEARNING_STEPS_MINUTES.get(0) * 3 / 2, 1440);
        } else {
            return RELEARNING_STEPS_MINUTES.get(currentStep);
        }
    }

    /**
     * 应用模糊系数（防止卡片堆积在同一日）
     */
    private int applyFuzz(int interval) {
        if (interval < 2) return interval;
        
        // ±5% 随机波动
        double fuzz = 0.95 + Math.random() * 0.1;
        return Math.max(1, (int) (interval * fuzz));
    }

    /**
     * 格式化分钟为易读文本
     */
    private String formatMinutes(int minutes) {
        if (minutes < 60) {
            return minutes + "分钟";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时";
        } else {
            return (minutes / 1440) + "天";
        }
    }
}
