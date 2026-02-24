package com.typingquiz.controller;

import com.typingquiz.dto.QuizDTO;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.service.QuizGroupService;
import com.typingquiz.service.QuizService;
import com.typingquiz.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
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
    private final QuizGroupService quizGroupService;

    @Autowired
    public ImportExportController(QuizService quizService, QuizGroupService quizGroupService) {
        this.quizService = quizService;
        this.quizGroupService = quizGroupService;
    }

    @GetMapping("/quiz/{id}/export")
    public ResponseEntity<QuizDTO> exportQuiz(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            Quiz quiz = quizService.getQuizById(id, userId);
            QuizDTO dto = quizService.convertToDTO(quiz);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quiz_" + id + ".json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/quizzes/export")
    public ResponseEntity<List<QuizDTO>> exportAllQuizzes(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Quiz> quizzes = quizService.getAllQuizzes(userId);
        List<QuizDTO> dtos = quizzes.stream()
                .map(quizService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"all_quizzes.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dtos);
    }

    @PostMapping("/quizzes/export")
    public ResponseEntity<List<QuizDTO>> exportQuizzesByIds(@RequestBody List<Long> quizIds, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Quiz> quizzes = quizService.getAllQuizzes(userId);
        // 只返回指定的测验
        List<Quiz> filtered = quizzes.stream()
                .filter(q -> quizIds.contains(q.getId()))
                .collect(Collectors.toList());
        List<QuizDTO> dtos = filtered.stream()
                .map(quizService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quizzes_batch.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dtos);
    }

    @GetMapping("/group/{groupId}/export")
    public ResponseEntity<List<QuizDTO>> exportQuizzesByGroup(@PathVariable Long groupId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            QuizGroup group = quizGroupService.getGroupById(groupId);
            // 验证用户身份
            if (!userId.equals(group.getUserId())) {
                return ResponseEntity.status(403).build();
            }
            List<Quiz> quizzes = group.getQuizzes();
            List<QuizDTO> dtos = quizzes.stream()
                    .map(quizService::convertToDTO)
                    .collect(Collectors.toList());
            String safeGroupName = group.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String filename = "group_" + safeGroupName + ".json";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/quiz/import")
    public ResponseEntity<String> importQuiz(@RequestBody QuizDTO quizDTO, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            Quiz quiz = quizService.createQuiz(quizDTO, userId);
            return ResponseEntity.ok("测验导入成功,ID: " + quiz.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/quizzes/import")
    public ResponseEntity<ImportResult> importQuizzes(@RequestBody List<QuizDTO> quizDTOs, HttpServletRequest request) {
        ImportResult result = new ImportResult();
        Long userId = getUserIdFromRequest(request);
        for (QuizDTO dto : quizDTOs) {
            try {
                Quiz quiz = quizService.createQuiz(dto, userId);
                result.addSuccess(quiz.getId(), dto.getTitle());
            } catch (Exception e) {
                result.addFailure(dto.getTitle(), e.getMessage());
            }
        }
        return ResponseEntity.ok(result);
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
