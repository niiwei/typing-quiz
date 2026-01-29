package com.typingquiz.service;

import com.typingquiz.dto.AnswerCreateDTO;
import com.typingquiz.dto.AnswerDTO;
import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.dto.QuizResponseDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizType;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.FillBlankQuizRepository;
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
    private final FillBlankQuizRepository fillBlankQuizRepository;
    private final FillBlankQuizService fillBlankQuizService;

    @Autowired
    public QuizService(QuizRepository quizRepository, 
                        AnswerRepository answerRepository,
                        FillBlankQuizRepository fillBlankQuizRepository,
                        FillBlankQuizService fillBlankQuizService) {
        this.quizRepository = quizRepository;
        this.answerRepository = answerRepository;
        this.fillBlankQuizRepository = fillBlankQuizRepository;
        this.fillBlankQuizService = fillBlankQuizService;
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
        
        // 设置测验类型
        QuizType quizType = quizDTO.getQuizType() != null ? quizDTO.getQuizType() : QuizType.TYPING;
        quiz.setQuizType(quizType);

        // 根据类型处理
        if (quizType == QuizType.FILL_BLANK) {
            // 填空题：不需要答案列表
            quiz = quizRepository.save(quiz);
            
            // 创建填空题信息
            if (quizDTO.getFillBlankQuiz() != null) {
                fillBlankQuizService.createFillBlankQuiz(quiz.getId(), quizDTO.getFillBlankQuiz());
            }
        } else {
            // 打字题：添加答案
            if (quizDTO.getAnswerList() != null && !quizDTO.getAnswerList().isEmpty()) {
                // 新格式：包含 content 和 comment
                for (AnswerCreateDTO answerDTO : quizDTO.getAnswerList()) {
                    if (answerDTO.getContent() != null && !answerDTO.getContent().trim().isEmpty()) {
                        Answer answer = new Answer(answerDTO.getContent());
                        answer.setComment(answerDTO.getComment());
                        quiz.addAnswer(answer);
                    }
                }
            } else if (quizDTO.getAnswers() != null) {
                // 旧格式：纯字符串数组
                for (String answerContent : quizDTO.getAnswers()) {
                    if (answerContent != null && !answerContent.trim().isEmpty()) {
                        Answer answer = new Answer(answerContent);
                        quiz.addAnswer(answer);
                    }
                }
            }
            quiz = quizRepository.save(quiz);
        }

        return quiz;
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

        // 更新测验类型
        if (quizDTO.getQuizType() != null) {
            quiz.setQuizType(quizDTO.getQuizType());
        }

        // 根据类型处理
        if (quiz.getQuizType() == QuizType.FILL_BLANK) {
            // 填空题：更新填空题信息
            if (quizDTO.getFillBlankQuiz() != null) {
                if (fillBlankQuizRepository.existsByQuizId(id)) {
                    fillBlankQuizService.updateFillBlankQuiz(id, quizDTO.getFillBlankQuiz());
                } else {
                    fillBlankQuizService.createFillBlankQuiz(id, quizDTO.getFillBlankQuiz());
                }
            }
        } else {
            // 打字题：更新答案列表
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
        
        // 删除关联的填空题信息
        if (fillBlankQuizRepository.existsByQuizId(id)) {
            fillBlankQuizRepository.deleteByQuizId(id);
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
        dto.setQuizType(quiz.getQuizType());
        
        // 如果是填空题，获取填空题信息
        if (quiz.getQuizType() == QuizType.FILL_BLANK) {
            try {
                FillBlankQuizDTO fillBlankDTO = fillBlankQuizService.getDTOByQuizId(quiz.getId());
                dto.setFillBlankQuiz(fillBlankDTO);
                dto.setTotalAnswers(fillBlankDTO.getBlanksCount());
            } catch (Exception e) {
                // 填空题信息不存在
            }
        }
        
        return dto;
    }

    /**
     * 将Answer实体列表转换为AnswerDTO列表
     */
    public List<AnswerDTO> toAnswerDTOList(List<Answer> answers) {
        return answers.stream()
                .map(answer -> new AnswerDTO(answer.getId(), answer.getContent(), answer.getComment()))
                .collect(Collectors.toList());
    }
}
