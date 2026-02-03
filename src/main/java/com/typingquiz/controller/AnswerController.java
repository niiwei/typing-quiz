package com.typingquiz.controller;

import com.typingquiz.dto.ValidationRequest;
import com.typingquiz.dto.ValidationResponse;
import com.typingquiz.entity.Answer;
import com.typingquiz.service.AnswerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    /**
     * 验证答案
     * POST /api/answers/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateAnswer(@RequestBody ValidationRequest request) {
        try {
            ValidationResponse response = answerService.validateAnswer(
                request.getQuizId(),
                request.getInput()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
    public ResponseEntity<List<Answer>> searchAnswers(@RequestParam String content) {
        List<Answer> answers = answerService.findAnswersByContent(content);
        return ResponseEntity.ok(answers);
    }
}
