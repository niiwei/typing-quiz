package com.typingquiz.controller;

import com.typingquiz.dto.AnswerDTO;
import com.typingquiz.dto.AnswerWithQuizDTO;
import com.typingquiz.dto.QuizResponseDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库管理控制器
 * 提供数据库查询和检索的REST API端点
 */
@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DatabaseController {

    private final QuizRepository quizRepository;
    private final AnswerRepository answerRepository;
    private final QuizService quizService;

    @Autowired
    public DatabaseController(QuizRepository quizRepository, 
                             AnswerRepository answerRepository,
                             QuizService quizService) {
        this.quizRepository = quizRepository;
        this.answerRepository = answerRepository;
        this.quizService = quizService;
    }

    /**
     * 按测验维度检索:根据测验ID查询其所有答案
     * GET /api/database/quiz/{id}/answers
     */
    @GetMapping("/quiz/{id}/answers")
    public ResponseEntity<List<AnswerDTO>> getAnswersByQuizId(@PathVariable Long id) {
        if (!quizRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Answer> answers = answerRepository.findByQuizId(id);
        List<AnswerDTO> response = answers.stream()
                .map(answer -> new AnswerDTO(answer.getId(), answer.getContent()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 按测验维度检索:根据测验名称查询测验及其答案
     * GET /api/database/quiz/search?name=xxx
     */
    @GetMapping("/quiz/search")
    public ResponseEntity<List<QuizResponseDTO>> searchQuizzesByName(
            @RequestParam String name) {
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Quiz> quizzes = quizRepository.findByTitleContainingIgnoreCase(name);
        List<QuizResponseDTO> response = quizzes.stream()
                .map(quizService::toResponseDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 按答案维度检索:根据答案ID查询其所属的测验
     * GET /api/database/answer/{id}/quiz
     */
    @GetMapping("/answer/{id}/quiz")
    public ResponseEntity<QuizResponseDTO> getQuizByAnswerId(@PathVariable Long id) {
        Answer answer = answerRepository.findById(id).orElse(null);
        
        if (answer == null) {
            return ResponseEntity.notFound().build();
        }
        
        Quiz quiz = answer.getQuiz();
        QuizResponseDTO response = quizService.toResponseDTO(quiz);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 按答案维度检索:根据答案内容查询答案及其所属测验
     * GET /api/database/answer/search?content=xxx
     */
    @GetMapping("/answer/search")
    public ResponseEntity<List<AnswerWithQuizDTO>> searchAnswersByContent(
            @RequestParam String content) {
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Answer> answers = answerRepository.findByContentContainingIgnoreCase(content);
        List<AnswerWithQuizDTO> response = answers.stream()
                .map(answer -> new AnswerWithQuizDTO(
                    answer.getId(),
                    answer.getContent(),
                    answer.getQuiz().getId(),
                    answer.getQuiz().getTitle()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取数据库统计信息
     * GET /api/database/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DatabaseStats> getDatabaseStats() {
        long totalQuizzes = quizRepository.count();
        long totalAnswers = answerRepository.count();
        
        DatabaseStats stats = new DatabaseStats(totalQuizzes, totalAnswers);
        return ResponseEntity.ok(stats);
    }

    /**
     * 数据库统计信息内部类
     */
    public static class DatabaseStats {
        private long totalQuizzes;
        private long totalAnswers;

        public DatabaseStats(long totalQuizzes, long totalAnswers) {
            this.totalQuizzes = totalQuizzes;
            this.totalAnswers = totalAnswers;
        }

        public long getTotalQuizzes() {
            return totalQuizzes;
        }

        public void setTotalQuizzes(long totalQuizzes) {
            this.totalQuizzes = totalQuizzes;
        }

        public long getTotalAnswers() {
            return totalAnswers;
        }

        public void setTotalAnswers(long totalAnswers) {
            this.totalAnswers = totalAnswers;
        }
    }
}
