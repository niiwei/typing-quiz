package com.typingquiz.controller;

import com.typingquiz.dto.AnswerDTO;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.dto.QuizResponseDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.service.QuizService;
import com.typingquiz.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测验控制器
 * 提供测验相关的REST API端点
 */
@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*")
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);

    private final QuizService quizService;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (JwtUtil.validateToken(token)) {
                return JwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }

    /**
     * 创建测验
     * POST /api/quizzes
     */
    @PostMapping
    public ResponseEntity<QuizResponseDTO> createQuiz(@RequestBody QuizDTO quizDTO, HttpServletRequest request) {
        try {
            logger.info("收到创建测验请求: title={}, quizType={}", quizDTO.getTitle(), quizDTO.getQuizType());
            Long userId = getUserIdFromRequest(request);
            logger.info("用户ID: {}", userId);
            Quiz quiz = quizService.createQuiz(quizDTO, userId);
            QuizResponseDTO response = quizService.toResponseDTO(quiz);
            logger.info("测验创建成功: ID={}", quiz.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("创建测验失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取测验详情
     * GET /api/quizzes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuizResponseDTO> getQuiz(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            Quiz quiz = quizService.getQuizById(id, userId);
            QuizResponseDTO response = quizService.toResponseDTO(quiz);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取所有测验（按用户过滤）
     * GET /api/quizzes
     */
    @GetMapping
    public ResponseEntity<List<QuizResponseDTO>> getAllQuizzes(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<QuizResponseDTO> response = quizService.getAllQuizDTOsForList(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取测验的所有答案
     * GET /api/quizzes/{id}/answers
     */
    @GetMapping("/{id}/answers")
    public ResponseEntity<List<AnswerDTO>> getQuizAnswers(@PathVariable Long id) {
        try {
            List<Answer> answers = quizService.getQuizAnswers(id);
            List<AnswerDTO> response = quizService.toAnswerDTOList(answers);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新测验
     * PUT /api/quizzes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuizResponseDTO> updateQuiz(
            @PathVariable Long id, 
            @RequestBody QuizDTO quizDTO,
            HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            Quiz quiz = quizService.updateQuiz(id, quizDTO, userId);
            QuizResponseDTO response = quizService.toResponseDTO(quiz);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除测验
     * DELETE /api/quizzes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            quizService.deleteQuiz(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
