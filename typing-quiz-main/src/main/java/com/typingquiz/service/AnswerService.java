package com.typingquiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.ValidationResponse;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 答案服务类
 * 处理答案验证和查询的业务逻辑
 */
@Service
@Transactional
public class AnswerService {

    private static final Logger logger = LoggerFactory.getLogger(AnswerService.class);

    private final AnswerRepository answerRepository;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnswerService(AnswerRepository answerRepository, QuizRepository quizRepository,
                         ObjectMapper objectMapper) {
        this.answerRepository = answerRepository;
        this.quizRepository = quizRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 验证答案(大小写不敏感)
     * @param quizId 测验ID
     * @param input 用户输入
     * @return 验证结果
     */
    public ValidationResponse validateAnswer(Long quizId, String input) {
        return validateAnswer(quizId, input, true, true, true);
    }

    public ValidationResponse validateAnswer(Long quizId, String input,
                                             Boolean ignorePunctuation,
                                             Boolean ignoreSpaces,
                                             Boolean ignoreCase) {
        logger.info("验证答案: quizId={}, input={}", quizId, input);
        
        // 验证输入
        if (input == null || input.trim().isEmpty()) {
            logger.info("输入为空，返回无效");
            return new ValidationResponse(false, null, null, false);
        }

        boolean punctuation = ignorePunctuation == null || ignorePunctuation;
        boolean spaces = ignoreSpaces == null || ignoreSpaces;
        boolean caseInsensitive = ignoreCase == null || ignoreCase;
        String normalizedInput = normalizeContent(input, punctuation, spaces, caseInsensitive);
        logger.info("标准化输入: {}", normalizedInput);

        // normalizedContent is kept for legacy search/indexing, but validation must use
        // the user's current settings rather than one persisted normalization policy.
        Optional<Answer> answerOpt = answerRepository.findByQuizId(quizId).stream()
                .filter(answer -> matchesAnswer(answer, normalizedInput, punctuation, spaces, caseInsensitive))
                .findFirst();
        
        logger.info("查询结果: {}", answerOpt.isPresent() ? "找到答案" : "未找到");

        if (answerOpt.isPresent()) {
            Answer answer = answerOpt.get();
            logger.info("答案ID: {}, 内容: {}", answer.getId(), answer.getContent());
            return new ValidationResponse(
                true,
                answer.getId(),
                answer.getContent(),  // 返回原始大小写
                false  // 前端负责跟踪已找到的答案
            );
        }

        return new ValidationResponse(false, null, null, false);
    }

    private boolean matchesAnswer(Answer answer, String normalizedInput,
                                  boolean ignorePunctuation, boolean ignoreSpaces, boolean ignoreCase) {
        if (normalizedInput.equals(normalizeContent(answer.getContent(), ignorePunctuation,
                ignoreSpaces, ignoreCase))) {
            return true;
        }
        if (!Integer.valueOf(2).equals(answer.getFormatVersion()) || answer.getPartsJson() == null) {
            return false;
        }
        try {
            JsonNode parts = objectMapper.readTree(answer.getPartsJson());
            for (JsonNode part : parts) {
                StringBuilder required = new StringBuilder();
                for (JsonNode segment : part.path("segments")) {
                    if ("required".equals(segment.path("kind").asText())) {
                        required.append(segment.path("text").asText());
                    }
                }
                if (required.length() > 0 && normalizedInput.equals(normalizeContent(required.toString(),
                        ignorePunctuation, ignoreSpaces, ignoreCase))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("答案要点结构无法解析: answerId={}", answer.getId(), e);
            return false;
        }
    }

    /**
     * 根据内容查询答案
     * @param content 答案内容
     * @return 匹配的答案列表
     */
    public List<Answer> findAnswersByContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }
        String normalizedContent = normalizeContent(content, true, true, true);
        return answerRepository.findByNormalizedContent(normalizedContent);
    }

    /**
     * 添加答案到测验
     * @param quizId 测验ID
     * @param content 答案内容
     * @return 创建的答案
     */
    public Answer addAnswerToQuiz(Long quizId, String content) {
        // 验证输入
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("答案内容不能为空");
        }

        // 查找测验
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + quizId));

        // 创建答案
        Answer answer = new Answer(content);
        quiz.addAnswer(answer);

        return answerRepository.save(answer);
    }

    /**
     * 标准化内容(转小写,去空格)
     * @param content 原始内容
     * @return 标准化后的内容
     */
    private String normalizeContent(String content, boolean ignorePunctuation,
                                    boolean ignoreSpaces, boolean ignoreCase) {
        if (content == null) {
            return "";
        }
        String result = content.replace("\uFEFF", "");
        if (ignorePunctuation) {
            result = result.replaceAll("[\\p{P}\\p{S}]", "");
        }
        if (ignoreSpaces) {
            result = result.codePoints()
                    .filter(codePoint -> !Character.isWhitespace(codePoint)
                            && !Character.isSpaceChar(codePoint))
                    .collect(StringBuilder::new,
                            StringBuilder::appendCodePoint,
                            StringBuilder::append)
                    .toString();
        } else {
            result = result.trim();
        }
        return ignoreCase ? result.toLowerCase(java.util.Locale.ROOT) : result;
    }
}
