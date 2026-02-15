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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 学习阶段服务
 * 处理新测验的学习流程（10min → 1h → 1d）
 */
@Service
@Transactional
public class LearningService {

    private static final Logger logger = LoggerFactory.getLogger(LearningService.class);

    // 学习间隔（分钟）：10分钟、60分钟（1小时）、1440分钟（1天）
    private static final List<Integer> LEARNING_STEPS_MINUTES = Arrays.asList(10, 60, 1440);
    
    // 简易度调整值
    private static final int EASE_DECREASE_AGAIN = 200;
    private static final int EASE_DECREASE_HARD = 150;
    private static final int EASE_INCREASE_EASY = 150;
    private static final int MIN_EASE = 1300;
    private static final int MAX_EASE = 5000;

    private final QuizReviewStatusRepository reviewStatusRepository;

    @Autowired
    public LearningService(QuizReviewStatusRepository reviewStatusRepository) {
        this.reviewStatusRepository = reviewStatusRepository;
    }

    /**
     * 获取用户指定测验的学习状态
     * 确保数据隔离：只能访问自己的复习状态
     */
    public QuizReviewStatus getLearningStatus(Long quizId, Long userId) {
        return reviewStatusRepository.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("未找到该测验的学习状态"));
    }

    /**
     * 提交学习评级
     * 
     * @param quizId 测验ID
     * @param userId 用户ID（数据隔离）
     * @param rating 评级：1=重来, 2=困难, 3=良好, 4=简单
     * @return 学习响应，包含下次学习时间
     */
    public LearnResponseDTO submitLearningRating(Long quizId, Long userId, int rating) {
        QuizReviewStatus status = getLearningStatus(quizId, userId);
        
        // 验证状态是否为学习中
        if (status.getStatus() != ReviewStatus.LEARNING && status.getStatus() != ReviewStatus.NEW) {
            throw new RuntimeException("该测验不在学习阶段，当前状态：" + status.getStatus());
        }

        // 如果是新卡片，第一次评级转为学习中
        if (status.getStatus() == ReviewStatus.NEW) {
            status.setStatus(ReviewStatus.LEARNING);
            status.setLearningStep(0);
        }

        int currentStep = status.getLearningStep();
        int totalSteps = LEARNING_STEPS_MINUTES.size();
        
        LearnResponseDTO response = new LearnResponseDTO();
        response.setQuizId(quizId);
        response.setRating(rating);

        switch (rating) {
            case 1: // 重来 - 返回第一步
                status.setLearningStep(0);
                status.setNextReviewDate(calculateNextReviewDate(LEARNING_STEPS_MINUTES.get(0)));
                response.setCompleted(false);
                response.setMessage("再来一次，10分钟后继续");
                response.setNextIntervalMinutes(LEARNING_STEPS_MINUTES.get(0));
                break;
                
            case 2: // 困难 - 重复当前步骤
                int hardDelay = calculateHardDelay(currentStep);
                status.setNextReviewDate(calculateNextReviewDate(hardDelay));
                response.setCompleted(false);
                response.setMessage("再复习一下，" + formatMinutes(hardDelay) + "后继续");
                response.setNextIntervalMinutes(hardDelay);
                break;
                
            case 3: // 良好 - 进入下一步或毕业
                if (currentStep >= totalSteps - 1) {
                    // 完成学习，进入复习阶段
                    graduateCard(status);
                    response.setCompleted(true);
                    response.setNewStatus(ReviewStatus.REVIEW);
                    response.setIntervalDays(status.getIntervalDays());
                    response.setMessage("学习完成！" + status.getIntervalDays() + "天后复习");
                } else {
                    // 进入下一步
                    int nextStep = currentStep + 1;
                    status.setLearningStep(nextStep);
                    int nextDelay = LEARNING_STEPS_MINUTES.get(nextStep);
                    status.setNextReviewDate(calculateNextReviewDate(nextDelay));
                    response.setCompleted(false);
                    response.setMessage("很好，" + formatMinutes(nextDelay) + "后继续");
                    response.setNextIntervalMinutes(nextDelay);
                }
                break;
                
            case 4: // 简单 - 直接毕业（跳过剩余步骤）
                // 使用简单间隔（4天）作为毕业间隔
                graduateWithEasyInterval(status);
                response.setCompleted(true);
                response.setNewStatus(ReviewStatus.REVIEW);
                response.setIntervalDays(status.getIntervalDays());
                response.setMessage("太棒了！" + status.getIntervalDays() + "天后复习");
                break;
                
            default:
                throw new IllegalArgumentException("无效的评级：" + rating);
        }

        // 更新统计
        status.setReviewCount(status.getReviewCount() + 1);
        status.setLastReviewDate(LocalDate.now());
        
        reviewStatusRepository.save(status);
        
        logger.info("用户{}完成测验{}的学习评级{}, 步骤{}, 下次复习时间:{}", 
                userId, quizId, rating, status.getLearningStep(), status.getNextReviewDate());
        
        return response;
    }

    /**
     * 计算困难按钮的延迟时间
     */
    private int calculateHardDelay(int currentStep) {
        if (currentStep == 0) {
            // 第一步时，困难延迟 = 1.5倍第一步间隔（最多1天）
            return Math.min(LEARNING_STEPS_MINUTES.get(0) * 3 / 2, 1440);
        } else {
            // 其他步骤，重复当前间隔
            return LEARNING_STEPS_MINUTES.get(currentStep);
        }
    }

    /**
     * 毕业卡片（完成学习阶段）
     */
    private void graduateCard(QuizReviewStatus status) {
        status.setStatus(ReviewStatus.REVIEW);
        status.setLearningStep(0);
        status.setIntervalDays(1); // 毕业间隔：1天
        status.setNextReviewDate(LocalDate.now().plusDays(1));
    }

    /**
     * 使用简单间隔毕业
     */
    private void graduateWithEasyInterval(QuizReviewStatus status) {
        status.setStatus(ReviewStatus.REVIEW);
        status.setLearningStep(0);
        status.setIntervalDays(4); // 简单间隔：4天
        status.setEaseFactor(Math.min(MAX_EASE, status.getEaseFactor() + EASE_INCREASE_EASY));
        status.setNextReviewDate(LocalDate.now().plusDays(4));
    }

    /**
     * 计算下次复习日期
     */
    private LocalDate calculateNextReviewDate(int minutesFromNow) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReview = now.plusMinutes(minutes);
        
        // 如果跨天了，返回日期；否则如果是今天，也返回今天
        return nextReview.toLocalDate();
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

    /**
     * 开始学习新测验
     * 将NEW状态转为LEARNING，设置第一步
     */
    public LearnResponseDTO startLearning(Long quizId, Long userId) {
        QuizReviewStatus status = getLearningStatus(quizId, userId);
        
        if (status.getStatus() != ReviewStatus.NEW) {
            throw new RuntimeException("该测验不是新测验，无法开始学习");
        }
        
        status.setStatus(ReviewStatus.LEARNING);
        status.setLearningStep(0);
        status.setNextReviewDate(calculateNextReviewDate(LEARNING_STEPS_MINUTES.get(0)));
        reviewStatusRepository.save(status);
        
        LearnResponseDTO response = new LearnResponseDTO();
        response.setQuizId(quizId);
        response.setCompleted(false);
        response.setMessage("开始学习，10分钟后将再次出现");
        response.setNextIntervalMinutes(LEARNING_STEPS_MINUTES.get(0));
        
        return response;
    }

    /**
     * 获取指定分组的新测验列表（用于学习）
     */
    public List<QuizReviewStatus> getNewCardsByGroup(Long groupId, Long userId) {
        // 这里需要关联查询分组和复习状态
        // 实现依赖于 QuizGroupRepository 的关联查询
        return reviewStatusRepository.findByUserIdAndStatusOrderByCreatedAtAsc(userId, ReviewStatus.NEW);
    }
}
