package com.typingquiz.controller;

import com.typingquiz.dto.LearnResponseDTO;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.service.LearningService;
import com.typingquiz.service.QuizReviewService;
import com.typingquiz.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 学习阶段控制器
 * 处理新测验的学习流程API
 */
@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private static final Logger logger = LoggerFactory.getLogger(LearningController.class);

    private final LearningService learningService;
    private final QuizReviewService quizReviewService;

    @Autowired
    public LearningController(LearningService learningService, QuizReviewService quizReviewService) {
        this.learningService = learningService;
        this.quizReviewService = quizReviewService;
    }

    /**
     * 获取当前用户ID
     * 从Authorization头中解析JWT Token
     */
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

    /**
     * 开始学习新测验
     * 将状态从NEW转为LEARNING
     */
    @PostMapping("/start/{quizId}")
    public ResponseEntity<LearnResponseDTO> startLearning(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            LearnResponseDTO response = learningService.startLearning(quizId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("开始学习失败: quizId={}, userId={}", quizId, userId, e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 提交学习评级
     * 
     * 请求体: { "rating": 1|2|3|4 }
     * 
     * rating含义:
     * 1 - 重来: 返回第一步
     * 2 - 困难: 重复当前步骤
     * 3 - 良好: 进入下一步或毕业
     * 4 - 简单: 直接毕业
     */
    @PostMapping("/{quizId}/rate")
    public ResponseEntity<LearnResponseDTO> submitRating(
            @PathVariable Long quizId,
            @RequestBody Map<String, Integer> request,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Integer rating = request.get("rating");
        if (rating == null || rating < 1 || rating > 4) {
            return ResponseEntity.badRequest().body(createErrorResponse("评级必须在1-4之间"));
        }

        try {
            LearnResponseDTO response = learningService.submitLearningRating(quizId, userId, rating);
            
            // 如果完成学习，获取今日剩余数量
            if (response.isCompleted()) {
                int remaining = quizReviewService.getRemainingCountForToday(userId);
                response.setRemainingToday(remaining);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("提交学习评级失败: quizId={}, userId={}, rating={}", quizId, userId, rating, e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取测验的学习状态
     */
    @GetMapping("/{quizId}/status")
    public ResponseEntity<Map<String, Object>> getLearningStatus(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            QuizReviewStatus status = learningService.getLearningStatus(quizId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("quizId", status.getQuizId());
            response.put("status", status.getStatus());
            response.put("learningStep", status.getLearningStep());
            response.put("nextReviewDate", status.getNextReviewDate());
            response.put("reviewCount", status.getReviewCount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取学习状态失败: quizId={}, userId={}", quizId, userId, e);
            return ResponseEntity.badRequest().body(createErrorMap(e.getMessage()));
        }
    }

    /**
     * 跳过当前测验（搁置到明天）
     */
    @PostMapping("/{quizId}/skip")
    public ResponseEntity<Map<String, Object>> skipCard(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            quizReviewService.buryQuiz(quizId, userId, 1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已搁置到明天");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("跳过测验失败: quizId={}, userId={}", quizId, userId, e);
            return ResponseEntity.badRequest().body(createErrorMap(e.getMessage()));
        }
    }

    private LearnResponseDTO createErrorResponse(String message) {
        LearnResponseDTO dto = new LearnResponseDTO();
        dto.setMessage(message);
        return dto;
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("error", message);
        return map;
    }
}
