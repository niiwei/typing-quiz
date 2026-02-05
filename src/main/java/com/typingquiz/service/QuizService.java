package com.typingquiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.AnswerCreateDTO;
import com.typingquiz.dto.AnswerDTO;
import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.dto.QuizResponseDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.entity.QuizType;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.FillBlankQuizRepository;
import com.typingquiz.repository.QuizGroupRepository;
import com.typingquiz.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final AnswerRepository answerRepository;
    private final FillBlankQuizRepository fillBlankQuizRepository;
    private final FillBlankQuizService fillBlankQuizService;
    private final QuizGroupRepository quizGroupRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public QuizService(QuizRepository quizRepository,
                        AnswerRepository answerRepository,
                        FillBlankQuizRepository fillBlankQuizRepository,
                        FillBlankQuizService fillBlankQuizService,
                        QuizGroupRepository quizGroupRepository,
                        ObjectMapper objectMapper) {
        this.quizRepository = quizRepository;
        this.answerRepository = answerRepository;
        this.fillBlankQuizRepository = fillBlankQuizRepository;
        this.fillBlankQuizService = fillBlankQuizService;
        this.quizGroupRepository = quizGroupRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建测验并保存答案
     */
    public Quiz createQuiz(QuizDTO quizDTO, Long userId) {
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
        
        // 绑定用户ID
        quiz.setUserId(userId);
        
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
            logger.info("导入打字题: {}, answerList={}, answers={}", 
                quizDTO.getTitle(), 
                quizDTO.getAnswerList() != null ? quizDTO.getAnswerList().size() : "null",
                quizDTO.getAnswers() != null ? quizDTO.getAnswers().size() : "null");
            
            if (quizDTO.getAnswerList() != null && !quizDTO.getAnswerList().isEmpty()) {
                // 新格式：包含 content 和 comment
                for (AnswerCreateDTO answerDTO : quizDTO.getAnswerList()) {
                    if (answerDTO.getContent() != null && !answerDTO.getContent().trim().isEmpty()) {
                        Answer answer = new Answer(answerDTO.getContent());
                        answer.setComment(answerDTO.getComment());
                        quiz.addAnswer(answer);
                        logger.info("添加答案: {}", answerDTO.getContent());
                    }
                }
            } else if (quizDTO.getAnswers() != null) {
                // 旧格式：纯字符串数组
                for (String answerContent : quizDTO.getAnswers()) {
                    if (answerContent != null && !answerContent.trim().isEmpty()) {
                        Answer answer = new Answer(answerContent);
                        quiz.addAnswer(answer);
                        logger.info("添加答案: {}", answerContent);
                    }
                }
            }
            quiz = quizRepository.save(quiz);
            logger.info("测验已保存, ID={}, 答案数量={}", quiz.getId(), quiz.getAnswers().size());
        }

        // 处理分组信息
        if (quizDTO.getGroups() != null && !quizDTO.getGroups().isEmpty()) {
            for (String groupName : quizDTO.getGroups()) {
                if (groupName == null || groupName.trim().isEmpty()) continue;

                QuizGroup group = quizGroupRepository.findByName(groupName.trim())
                        .orElseGet(() -> {
                            QuizGroup newGroup = new QuizGroup(groupName.trim(), "");
                            return quizGroupRepository.save(newGroup);
                        });
                
                group.addQuiz(quiz);
                quizGroupRepository.save(group);
            }
        }

        return quiz;
    }

    /**
     * 根据ID获取测验详情
     */
    public Quiz getQuizById(Long id) {
        return quizRepository.findByIdWithAnswers(id)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + id));
    }

    /**
     * 获取所有测验（按用户过滤）
     */
    public List<Quiz> getAllQuizzes(Long userId) {
        if (userId == null) {
            return quizRepository.findAllWithAnswers();
        }
        return quizRepository.findByUserIdWithAnswers(userId);
    }

    /**
     * 获取所有测验（管理员用，不过滤用户）
     */
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAllWithAnswers();
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

    public QuizDTO convertToDTO(Quiz quiz) {
        QuizDTO dto = new QuizDTO();
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setTimeLimit(quiz.getTimeLimit());
        dto.setQuizType(quiz.getQuizType());

        if (quiz.getGroups() != null) {
            List<String> groupNames = quiz.getGroups().stream()
                    .map(QuizGroup::getName)
                    .collect(Collectors.toList());
            dto.setGroups(groupNames);
        }

        List<String> answers = quiz.getAnswers().stream()
                .map(Answer::getContent)
                .collect(Collectors.toList());
        dto.setAnswers(answers);

        List<AnswerCreateDTO> answerList = quiz.getAnswers().stream()
                .map(a -> new AnswerCreateDTO(a.getContent(), a.getComment()))
                .collect(Collectors.toList());
        dto.setAnswerList(answerList);

        if (quiz.getQuizType() == QuizType.FILL_BLANK) {
            fillBlankQuizRepository.findByQuizId(quiz.getId()).ifPresent(fillBlankQuiz -> {
                FillBlankQuizDTO fillBlankDTO = new FillBlankQuizDTO();
                fillBlankDTO.setId(fillBlankQuiz.getId());
                fillBlankDTO.setQuizId(fillBlankQuiz.getQuizId());
                fillBlankDTO.setFullText(fillBlankQuiz.getFullText());
                fillBlankDTO.setDisplayText(fillBlankQuiz.getDisplayText());
                fillBlankDTO.setBlanksCount(fillBlankQuiz.getBlanksCount());

                try {
                    List<FillBlankQuizDTO.BlankInfo> blanks = objectMapper.readValue(
                            fillBlankQuiz.getBlanksInfo(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, FillBlankQuizDTO.BlankInfo.class)
                    );
                    fillBlankDTO.setBlanks(blanks);
                } catch (Exception e) {
                    fillBlankDTO.setBlanks(new ArrayList<>());
                }

                dto.setFillBlankQuiz(fillBlankDTO);
            });
        }

        return dto;
    }
}
