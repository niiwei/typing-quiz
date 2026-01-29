package com.typingquiz.controller;

import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.entity.FillBlankQuiz;
import com.typingquiz.service.FillBlankQuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 填空题控制器
 */
@RestController
@RequestMapping("/api/fill-blank")
public class FillBlankQuizController {

    private final FillBlankQuizService fillBlankQuizService;

    @Autowired
    public FillBlankQuizController(FillBlankQuizService fillBlankQuizService) {
        this.fillBlankQuizService = fillBlankQuizService;
    }

    /**
     * 获取测验的填空题详情
     */
    @GetMapping("/quiz/{quizId}")
    public FillBlankQuizDTO getByQuizId(@PathVariable Long quizId) {
        return fillBlankQuizService.getDTOByQuizId(quizId);
    }

    /**
     * 创建填空题
     */
    @PostMapping("/quiz/{quizId}")
    public FillBlankQuizDTO create(@PathVariable Long quizId, @RequestBody FillBlankQuizDTO dto) {
        return fillBlankQuizService.toDTO(fillBlankQuizService.createFillBlankQuiz(quizId, dto));
    }

    /**
     * 更新填空题
     */
    @PutMapping("/quiz/{quizId}")
    public FillBlankQuizDTO update(@PathVariable Long quizId, @RequestBody FillBlankQuizDTO dto) {
        return fillBlankQuizService.toDTO(fillBlankQuizService.updateFillBlankQuiz(quizId, dto));
    }

    /**
     * 删除填空题
     */
    @DeleteMapping("/quiz/{quizId}")
    public ResponseEntity<Void> delete(@PathVariable Long quizId) {
        fillBlankQuizService.deleteByQuizId(quizId);
        return ResponseEntity.ok().build();
    }
}
