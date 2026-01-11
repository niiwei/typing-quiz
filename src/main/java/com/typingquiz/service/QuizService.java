package com.typingquiz.service;

import com.typingquiz.dto.AnswerDTO;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.dto.QuizResponseDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 测验服务类
 * 处理测验相关的业务逻辑
 */
@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final AnswerRepository answerRepository;

    @Autowired
    public QuizService(QuizRepository quizRepository, AnswerRepository answerRepository) {
        this.quizRepository = quizRepository;
        this.answerRepository = answerRepository;
    }

    /**
     * 创建测验并保存答案
     */
    public Quiz createQuiz(QuizDTO quizDTO) {
        // 验证输入
        if (quizDTO.getTitle() == null || quizDTO.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("测验标题不能为空");
        }
        
        if (quizDTO.getTimeLimit() != null && quizDTO.getTimeLimit() < 0) {
            throw new IllegalArgumentException("时间限制不能为负数");
        }

        // 创建测验实体
        Quiz quiz = new Quiz(
            quizDTO.getTitle(),
            quizDTO.getDescription(),
            quizDTO.getTimeLimit()
        );

        // 添加答案
        if (quizDTO.getAnswers() != null) {
            for (String answerContent : quizDTO.getAnswers()) {
                if (answerContent != null && !answerContent.trim().isEmpty()) {
                    Answer answer = new Answer(answerContent);
                    quiz.addAnswer(answer);
                }
            }
        }

        return quizRepository.save(quiz);
    }

    /**
     * 根据ID获取测验详情
     */
    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + id));
    }

    /**
     * 获取所有测验
     */
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    /**
     * 获取测验的所有答案
     */
    public List<Answer> getQuizAnswers(Long quizId) {
        // 验证测验是否存在
        if (!quizRepository.existsById(quizId)) {
            throw new RuntimeException("测验不存在: ID=" + quizId);
        }
        return answerRepository.findByQuizId(quizId);
    }

    /**
     * 更新测验
     */
    public Quiz updateQuiz(Long id, QuizDTO quizDTO) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + id));
        
        // 验证输入
        if (quizDTO.getTitle() == null || quizDTO.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("测验标题不能为空");
        }
        
        if (quizDTO.getTimeLimit() != null && quizDTO.getTimeLimit() < 0) {
            throw new IllegalArgumentException("时间限制不能为负数");
        }

        // 更新基本信息
        quiz.setTitle(quizDTO.getTitle());
        quiz.setDescription(quizDTO.getDescription());
        quiz.setTimeLimit(quizDTO.getTimeLimit());

        // 如果提供了新的答案列表,更新答案
        if (quizDTO.getAnswers() != null) {
            // 清除旧答案
            quiz.getAnswers().clear();
            
            // 添加新答案
            for (String answerContent : quizDTO.getAnswers()) {
                if (answerContent != null && !answerContent.trim().isEmpty()) {
                    Answer answer = new Answer(answerContent);
                    quiz.addAnswer(answer);
                }
            }
        }

        return quizRepository.save(quiz);
    }

    /**
     * 删除测验
     */
    public void deleteQuiz(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new RuntimeException("测验不存在: ID=" + id);
        }
        quizRepository.deleteById(id);
    }

    /**
     * 将Quiz实体转换为QuizResponseDTO
     */
    public QuizResponseDTO toResponseDTO(Quiz quiz) {
        QuizResponseDTO dto = new QuizResponseDTO();
        dto.setId(quiz.getId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setTimeLimit(quiz.getTimeLimit());
        dto.setTotalAnswers(quiz.getAnswers() != null ? quiz.getAnswers().size() : 0);
        dto.setCreatedAt(quiz.getCreatedAt());
        return dto;
    }

    /**
     * 将Answer实体列表转换为AnswerDTO列表
     */
    public List<AnswerDTO> toAnswerDTOList(List<Answer> answers) {
        return answers.stream()
                .map(answer -> new AnswerDTO(answer.getId(), answer.getContent()))
                .collect(Collectors.toList());
    }
}
