package com.typingquiz.controller;

import com.typingquiz.dto.ValidationRequest;
import com.typingquiz.dto.ValidationResponse;
import com.typingquiz.entity.Answer;
import com.typingquiz.service.AnswerService;
import com.typingquiz.service.QuizService;
import com.typingquiz.config.ApiAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 答案控制器
 * 提供答案验证和查询的REST API端点
 */
@RestController
@RequestMapping("/api/answers")
@CrossOrigin(origins = "*")
public class AnswerController {

    private static final Logger logger = LoggerFactory.getLogger(AnswerController.class);

    private final AnswerService answerService;
    private final QuizService quizService;

    @Autowired
    public AnswerController(AnswerService answerService, QuizService quizService) {
        this.answerService = answerService;
        this.quizService = quizService;
    }

    /**
     * 验证答案
     * POST /api/answers/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateAnswer(@RequestBody ValidationRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute(ApiAuthenticationFilter.USER_ID_ATTRIBUTE);
            quizService.getQuizById(request.getQuizId(), userId);
            ValidationResponse response = answerService.validateAnswer(
                request.getQuizId(),
                request.getInput(),
                request.getIgnorePunctuation(),
                request.getIgnoreSpaces(),
                request.getIgnoreCase()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e instanceof RuntimeException && e.getMessage() != null
                    && (e.getMessage().contains("测验不存在") || e.getMessage().contains("无权访问"))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ValidationResponse(false, null, null, false));
            }
            logger.error("验证答案失败: quizId={}, input={}",
                    request != null ? request.getQuizId() : null,
                    request != null ? request.getInput() : null,
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ValidationResponse(false, null, null, false));
        }
    }

    /**
     * 搜索答案
     * GET /api/answers/search?content=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<List<Answer>> searchAnswers(@RequestParam String content, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(ApiAuthenticationFilter.USER_ID_ATTRIBUTE);
        List<Answer> answers = answerService.findAnswersByContent(content, userId);
        return ResponseEntity.ok(answers);
    }
}
