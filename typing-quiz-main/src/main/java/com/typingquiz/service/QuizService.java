package com.typingquiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.AnswerCreateDTO;
import com.typingquiz.dto.AnswerDTO;
import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.dto.QuizResponseDTO;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.FillBlankQuiz;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.entity.QuizType;
import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.FillBlankQuizRepository;
import com.typingquiz.repository.QuizGroupRepository;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.repository.QuizReviewStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final QuizReviewStatusRepository quizReviewStatusRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public QuizService(QuizRepository quizRepository,
                        AnswerRepository answerRepository,
                        FillBlankQuizRepository fillBlankQuizRepository,
                        FillBlankQuizService fillBlankQuizService,
                        QuizGroupRepository quizGroupRepository,
                        QuizReviewStatusRepository quizReviewStatusRepository,
                        ObjectMapper objectMapper) {
        this.quizRepository = quizRepository;
        this.answerRepository = answerRepository;
        this.fillBlankQuizRepository = fillBlankQuizRepository;
        this.fillBlankQuizService = fillBlankQuizService;
        this.quizGroupRepository = quizGroupRepository;
        this.quizReviewStatusRepository = quizReviewStatusRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建测验并保存答案
     */
    public Quiz createQuiz(QuizDTO quizDTO, Long userId) {
        logger.info("开始创建测验: title={}, userId={}", quizDTO.getTitle(), userId);
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
        logger.info("测验类型: {}", quizType);

        // 根据类型处理
        if (quizType == QuizType.FILL_BLANK) {
            // 填空题：不需要答案列表
            quiz = quizRepository.save(quiz);
            logger.info("已保存填空题基础信息, ID={}", quiz.getId());
            
            // 创建填空题信息
            if (quizDTO.getFillBlankQuiz() != null) {
                fillBlankQuizService.createFillBlankQuiz(quiz.getId(), quizDTO.getFillBlankQuiz());
                logger.info("已创建填空题详细信息");
            }
        } else {
            // 打字题：添加答案
            logger.info("导入打字题: {}, answerList={}", 
                quizDTO.getTitle(), 
                quizDTO.getAnswerList() != null ? quizDTO.getAnswerList().size() : "null");
            
            // 用于去重已添加的答案内容
            java.util.Set<String> addedAnswers = new java.util.HashSet<>();
            
            if (quizDTO.getAnswerList() != null && !quizDTO.getAnswerList().isEmpty()) {
                // 新格式：包含 content 和 comment
                for (AnswerCreateDTO answerDTO : quizDTO.getAnswerList()) {
                    if (answerDTO.getContent() != null && !answerDTO.getContent().trim().isEmpty()) {
                        String content = answerDTO.getContent().trim();
                        if (!addedAnswers.contains(content)) {
                            Answer answer = new Answer(content);
                            answer.setComment(answerDTO.getComment());
                            quiz.addAnswer(answer);
                            addedAnswers.add(content);
                            logger.info("添加答案: {}", content);
                        }
                    }
                }
            } else if (quizDTO.getAnswers() != null) {
                // 旧格式：纯字符串数组（兼容旧数据）
                for (String answerContent : quizDTO.getAnswers()) {
                    if (answerContent != null && !answerContent.trim().isEmpty()) {
                        String content = answerContent.trim();
                        if (!addedAnswers.contains(content)) {
                            Answer answer = new Answer(content);
                            quiz.addAnswer(answer);
                            addedAnswers.add(content);
                            logger.info("添加答案: {}", content);
                        }
                    }
                }
            }
            quiz = quizRepository.save(quiz);
            logger.info("测验已保存, ID={}, 答案数量={}", quiz.getId(), quiz.getAnswers().size());
        }

        // 处理分组信息
        if (quizDTO.getGroups() != null && !quizDTO.getGroups().isEmpty()) {
            // 用户指定了分组，使用用户指定的分组
            for (String groupName : quizDTO.getGroups()) {
                if (groupName == null || groupName.trim().isEmpty()) continue;

                List<QuizGroup> existingGroups = quizGroupRepository.findByNameAndUserId(groupName.trim(), userId);
                QuizGroup group;
                if (existingGroups.isEmpty()) {
                    // 创建新分组
                    group = new QuizGroup(groupName.trim(), "");
                    group.setUserId(userId);
                    group = quizGroupRepository.save(group);
                } else {
                    // 使用第一个匹配的分组
                    group = existingGroups.get(0);
                }
                
                group.addQuiz(quiz);
                quizGroupRepository.save(group);
            }
        } else {
            // 未指定分组，自动关联到默认分组
            QuizGroup defaultGroup = quizGroupRepository.findByUserIdAndName(userId, "默认分组")
                    .orElseGet(() -> {
                        // 如果默认分组不存在，创建它
                        QuizGroup group = new QuizGroup("默认分组", "系统自动创建的默认分组");
                        group.setUserId(userId);
                        return quizGroupRepository.save(group);
                    });
            defaultGroup.addQuiz(quiz);
            quizGroupRepository.save(defaultGroup);
            logger.info("测验 {} 已自动关联到默认分组", quiz.getId());
        }

        // 自动创建复习状态（确保每个测验都是未学习状态）
        logger.info("正在为测验 {} 创建复习状态, userId={}", quiz.getId(), userId);
        createReviewStatusForQuiz(quiz.getId(), userId);

        logger.info("测验创建流程完成: ID={}", quiz.getId());
        return quiz;
    }

    /**
     * 为测验创建复习状态（初始状态：NEW）
     */
    private void createReviewStatusForQuiz(Long quizId, Long userId) {
        try {
            // 检查是否已存在（避免重复创建）
            if (!quizReviewStatusRepository.existsByQuizIdAndUserId(quizId, userId)) {
                QuizReviewStatus status = new QuizReviewStatus(quizId, userId);
                status.setStatus(ReviewStatus.NEW);
                quizReviewStatusRepository.save(status);
                logger.info("已为测验 {} 创建复习状态", quizId);
            }
        } catch (Exception e) {
            logger.warn("创建测验 {} 的复习状态失败: {}", quizId, e.getMessage());
            // 不影响主流程，继续执行
        }
    }

    /**
     * 根据ID获取测验详情（带用户验证）
     */
    @Transactional(readOnly = true)
    public Quiz getQuizById(Long id, Long userId) {
        logger.info("[QuizService.getQuizById] 查找测验 ID={}, userId={}", id, userId);
        
        // 使用基础的 findById
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        
        if (!quizOpt.isPresent()) {
            logger.warn("[QuizService.getQuizById] 数据库基础查询找不到测验 ID={}", id);
            throw new RuntimeException("测验不存在: ID=" + id);
        }
        
        Quiz quiz = quizOpt.get();
        logger.info("[QuizService.getQuizById] 找到测验: title={}, quizUserId={}", quiz.getTitle(), quiz.getUserId());
        
        // 验证用户身份
        if (userId != null && quiz.getUserId() != null && !userId.equals(quiz.getUserId())) {
            logger.error("[QuizService.getQuizById] 用户越权访问: userId={}, quizUserId={}, quizId={}", 
                userId, quiz.getUserId(), id);
            throw new RuntimeException("无权访问此测验");
        }
        
        return quiz;
    }

    /**
     * 根据ID获取测验详情DTO（带用户验证，在事务内完成所有操作）
     * 避免跨事务边界访问懒加载集合导致的事务回滚问题
     */
    @Transactional(readOnly = true)
    public QuizResponseDTO getQuizDTOById(Long id, Long userId) {
        logger.info("[QuizService.getQuizDTOById] 查找测验 ID={}, userId={}", id, userId);
        
        // 使用 findByIdWithAnswers 预加载答案
        Optional<Quiz> quizOpt = quizRepository.findByIdWithAnswers(id);
        
        if (!quizOpt.isPresent()) {
            logger.warn("[QuizService.getQuizDTOById] 数据库中找不到测验 ID={}", id);
            throw new RuntimeException("测验不存在: ID=" + id);
        }
        
        Quiz quiz = quizOpt.get();
        logger.info("[QuizService.getQuizDTOById] 找到测验: title={}, quizUserId={}", quiz.getTitle(), quiz.getUserId());
        
        // 验证用户身份
        if (userId != null && quiz.getUserId() != null && !userId.equals(quiz.getUserId())) {
            logger.error("[QuizService.getQuizDTOById] 用户越权访问: userId={}, quizUserId={}, quizId={}", 
                userId, quiz.getUserId(), id);
            throw new RuntimeException("无权访问此测验");
        }
        
        // 在事务内手动构建DTO，避免调用可能触发事务回滚的其他服务方法
        QuizResponseDTO dto = new QuizResponseDTO();
        dto.setId(quiz.getId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setTimeLimit(quiz.getTimeLimit());
        dto.setTotalAnswers(quiz.getAnswers() != null ? quiz.getAnswers().size() : 0);
        dto.setCreatedAt(quiz.getCreatedAt());
        dto.setQuizType(quiz.getQuizType());
        
        // 填充答案数据
        if (quiz.getAnswers() != null && !quiz.getAnswers().isEmpty()) {
            List<String> answers = quiz.getAnswers().stream()
                    .map(answer -> answer.getContent())
                    .collect(Collectors.toList());
            dto.setAnswers(answers);
            
            List<AnswerCreateDTO> answerList = quiz.getAnswers().stream()
                    .map(answer -> new AnswerCreateDTO(answer.getContent(), answer.getComment()))
                    .collect(Collectors.toList());
            dto.setAnswerList(answerList);
        }
        
        // 如果是填空题，直接使用仓库查询（避免服务层事务问题）
        if (quiz.getQuizType() == QuizType.FILL_BLANK) {
            try {
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
                    dto.setTotalAnswers(fillBlankQuiz.getBlanksCount());
                });
            } catch (Exception e) {
                logger.warn("[QuizService.getQuizDTOById] 填空题信息加载失败: id={}, error={}", id, e.getMessage());
                // 填空题信息不存在，忽略错误
            }
        }
        
        logger.info("[QuizService.getQuizDTOById] DTO构建成功: id={}, title={}", dto.getId(), dto.getTitle());
        return dto;
    }

    /**
     * 获取所有测验（按用户过滤）- 用于列表展示，优化查询
     */
    public List<Quiz> getAllQuizzes(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return quizRepository.findByUserIdSimple(userId);
    }

    /**
     * 获取所有测验DTO（按用户过滤）- 批量查询答案数量，避免N+1
     */
    public List<QuizResponseDTO> getAllQuizDTOsForList(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 查询测验列表
        List<Quiz> quizzes = quizRepository.findByUserIdSimple(userId);
        
        // 批量查询打字题答案数量
        List<Object[]> answerCounts = quizRepository.findAnswerCountsByUserId(userId);
        Map<Long, Integer> countMap = new HashMap<>();
        for (Object[] row : answerCounts) {
            countMap.put((Long) row[0], ((Long) row[1]).intValue());
        }
        
        // 批量查询填空题挖空数量
        List<FillBlankQuiz> fillBlankQuizzes = fillBlankQuizRepository.findAll();
        Map<Long, Integer> blanksCountMap = new HashMap<>();
        for (FillBlankQuiz fb : fillBlankQuizzes) {
            blanksCountMap.put(fb.getQuizId(), fb.getBlanksCount());
        }
        
        // 组装DTO
        return quizzes.stream().map(quiz -> {
            QuizResponseDTO dto = new QuizResponseDTO();
            dto.setId(quiz.getId());
            dto.setTitle(quiz.getTitle());
            dto.setDescription(quiz.getDescription());
            dto.setTimeLimit(quiz.getTimeLimit());
            
            // 填空题使用blanksCount，打字题使用answers数量
            if (quiz.getQuizType() == QuizType.FILL_BLANK) {
                dto.setTotalAnswers(blanksCountMap.getOrDefault(quiz.getId(), 0));
            } else {
                dto.setTotalAnswers(countMap.getOrDefault(quiz.getId(), 0));
            }
            
            dto.setCreatedAt(quiz.getCreatedAt());
            dto.setQuizType(quiz.getQuizType());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取所有测验（带答案，用于编辑/导出）
     */
    public List<Quiz> getAllQuizzesWithAnswers(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
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
     * 更新测验（带用户验证）
     */
    public Quiz updateQuiz(Long id, QuizDTO quizDTO, Long userId) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + id));
        // 验证用户身份
        if (userId != null && !userId.equals(quiz.getUserId())) {
            throw new RuntimeException("无权修改此测验");
        }
        
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
            // 检查answerList（包含注释的答案列表）而非answers
            if (quizDTO.getAnswerList() != null && !quizDTO.getAnswerList().isEmpty()) {
                // 清除旧答案
                quiz.getAnswers().clear();
                
                // 添加新答案
                for (AnswerCreateDTO answerDTO : quizDTO.getAnswerList()) {
                    if (answerDTO.getContent() != null && !answerDTO.getContent().trim().isEmpty()) {
                        Answer answer = new Answer(answerDTO.getContent());
                        answer.setComment(answerDTO.getComment());
                        quiz.addAnswer(answer);
                    }
                }
            } else if (quizDTO.getAnswers() != null) {
                // 兼容旧版前端，只包含答案内容
                quiz.getAnswers().clear();
                
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
     * 删除测验（带用户验证）
     */
    public void deleteQuiz(Long id, Long userId) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + id));
        // 验证用户身份
        if (userId != null && !userId.equals(quiz.getUserId())) {
            throw new RuntimeException("无权删除此测验");
        }
        
        // 先移除所有分组关联（避免外键约束冲突）
        List<QuizGroup> groups = quizGroupRepository.findByQuizzesId(id);
        for (QuizGroup group : groups) {
            group.getQuizzes().remove(quiz);
            quizGroupRepository.save(group);
        }

        // 删除关联的填空题信息
        if (fillBlankQuizRepository.existsByQuizId(id)) {
            fillBlankQuizRepository.deleteByQuizId(id);
        }

        // 删除关联的复习状态记录
        quizReviewStatusRepository.deleteByQuizId(id);

        quizRepository.deleteById(id);
    }

    /**
     * 将Quiz实体转换为QuizResponseDTO（完整版，用于编辑页面）
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
        
        // 填充答案数据（用于编辑页面）
        if (quiz.getAnswers() != null && !quiz.getAnswers().isEmpty()) {
            List<String> answers = quiz.getAnswers().stream()
                    .map(answer -> answer.getContent())
                    .collect(Collectors.toList());
            dto.setAnswers(answers);
            
            List<AnswerCreateDTO> answerList = quiz.getAnswers().stream()
                    .map(answer -> new AnswerCreateDTO(answer.getContent(), answer.getComment()))
                    .collect(Collectors.toList());
            dto.setAnswerList(answerList);
        }
        
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

        // 只保留answerList字段用于导出（包含注释信息）
        if (quiz.getAnswers() != null && !quiz.getAnswers().isEmpty()) {
            List<AnswerCreateDTO> answerList = quiz.getAnswers().stream()
                    .map(a -> new AnswerCreateDTO(a.getContent(), a.getComment()))
                    .collect(Collectors.toList());
            dto.setAnswerList(answerList);
        }

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
