package com.typingquiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 导入导出控制器
 * 提供测验数据的导入和导出功能
 */
@RestController
@RequestMapping("/api/import-export")
@CrossOrigin(origins = "*")
public class ImportExportController {

    private final QuizService quizService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ImportExportController(QuizService quizService, ObjectMapper objectMapper) {
        this.quizService = quizService;
        this.objectMapper = objectMapper;
    }

    /**
     * 导出单个测验为JSON
     * GET /api/import-export/quiz/{id}/export
     */
    @GetMapping("/quiz/{id}/export")
    public ResponseEntity<QuizDTO> exportQuiz(@PathVariable Long id) {
        try {
            Quiz quiz = quizService.getQuizById(id);
            QuizDTO dto = convertToDTO(quiz);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"quiz_" + id + ".json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 导出所有测验为JSON
     * GET /api/import-export/quizzes/export
     */
    @GetMapping("/quizzes/export")
    public ResponseEntity<List<QuizDTO>> exportAllQuizzes() {
        List<Quiz> quizzes = quizService.getAllQuizzes();
        List<QuizDTO> dtos = quizzes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"all_quizzes.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dtos);
    }

    /**
     * 导入单个测验
     * POST /api/import-export/quiz/import
     */
    @PostMapping("/quiz/import")
    public ResponseEntity<String> importQuiz(@RequestBody QuizDTO quizDTO) {
        try {
            Quiz quiz = quizService.createQuiz(quizDTO);
            return ResponseEntity.ok("测验导入成功,ID: " + quiz.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("导入失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入测验
     * POST /api/import-export/quizzes/import
     */
    @PostMapping("/quizzes/import")
    public ResponseEntity<ImportResult> importQuizzes(@RequestBody List<QuizDTO> quizDTOs) {
        ImportResult result = new ImportResult();
        
        for (QuizDTO dto : quizDTOs) {
            try {
                Quiz quiz = quizService.createQuiz(dto);
                result.addSuccess(quiz.getId(), dto.getTitle());
            } catch (Exception e) {
                result.addFailure(dto.getTitle(), e.getMessage());
            }
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 将Quiz实体转换为QuizDTO
     */
    private QuizDTO convertToDTO(Quiz quiz) {
        QuizDTO dto = new QuizDTO();
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setTimeLimit(quiz.getTimeLimit());
        
        List<String> answers = quiz.getAnswers().stream()
                .map(Answer::getContent)
                .collect(Collectors.toList());
        dto.setAnswers(answers);
        
        return dto;
    }

    /**
     * 导入结果类
     */
    public static class ImportResult {
        private List<SuccessItem> successes = new ArrayList<>();
        private List<FailureItem> failures = new ArrayList<>();

        public void addSuccess(Long id, String title) {
            successes.add(new SuccessItem(id, title));
        }

        public void addFailure(String title, String error) {
            failures.add(new FailureItem(title, error));
        }

        public List<SuccessItem> getSuccesses() {
            return successes;
        }

        public List<FailureItem> getFailures() {
            return failures;
        }

        public int getSuccessCount() {
            return successes.size();
        }

        public int getFailureCount() {
            return failures.size();
        }

        public static class SuccessItem {
            private Long id;
            private String title;

            public SuccessItem(Long id, String title) {
                this.id = id;
                this.title = title;
            }

            public Long getId() {
                return id;
            }

            public String getTitle() {
                return title;
            }
        }

        public static class FailureItem {
            private String title;
            private String error;

            public FailureItem(String title, String error) {
                this.title = title;
                this.error = error;
            }

            public String getTitle() {
                return title;
            }

            public String getError() {
                return error;
            }
        }
    }
}
