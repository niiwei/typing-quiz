package com.typingquiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.ValidationResponse;
import com.typingquiz.dto.AnswerMatchDTO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
        if (normalizedInput.isEmpty()) {
            logger.info("输入在当前规范化设置下为空，返回无效");
            return new ValidationResponse(false, null, null, false);
        }

        // normalizedContent is kept for legacy search/indexing, but validation must use
        // the user's current settings rather than one persisted normalization policy.
        List<AnswerMatchDTO> matches = findMatches(quizId, normalizedInput, punctuation, spaces, caseInsensitive);
        
        logger.info("查询结果: {}", matches.isEmpty() ? "未找到" : "找到答案");

        if (!matches.isEmpty()) {
            AnswerMatchDTO first = matches.get(0);
            ValidationResponse response = new ValidationResponse(true, first.getAnswerId(),
                    first.getDisplayContent(), false);
            response.setMatches(matches);
            return response;
        }

        return new ValidationResponse(false, null, null, false);
    }

    private List<AnswerMatchDTO> findMatches(Long quizId, String normalizedInput,
                                              boolean ignorePunctuation, boolean ignoreSpaces,
                                              boolean ignoreCase) {
        Map<Long, AnswerMatchDTO> matches = new LinkedHashMap<>();
        for (Answer answer : answerRepository.findByQuizId(quizId)) {
            List<Integer> partIndices = new ArrayList<>();
            if (normalizedInput.equals(normalizeContent(answer.getContent(), ignorePunctuation,
                    ignoreSpaces, ignoreCase))) {
                if (Integer.valueOf(2).equals(answer.getFormatVersion()) && answer.getPartsJson() != null) {
                    partIndices.addAll(allPartIndices(answer));
                } else {
                    partIndices.add(0);
                }
            } else if (Integer.valueOf(2).equals(answer.getFormatVersion()) && answer.getPartsJson() != null) {
                String requiredContent = requiredContent(answer);
                if (normalizedInput.equals(normalizeContent(requiredContent, ignorePunctuation,
                        ignoreSpaces, ignoreCase))) {
                    partIndices.addAll(allPartIndices(answer));
                } else {
                    partIndices.addAll(matchingPartIndices(answer, normalizedInput, ignorePunctuation,
                            ignoreSpaces, ignoreCase));
                }
            }
            if (!partIndices.isEmpty()) {
                matches.put(answer.getId(), new AnswerMatchDTO(answer.getId(), answer.getContent(), partIndices));
            }
        }
        return new ArrayList<>(matches.values());
    }

    private List<Integer> allPartIndices(Answer answer) {
        List<Integer> indices = new ArrayList<>();
        try {
            JsonNode parts = objectMapper.readTree(answer.getPartsJson());
            for (int i = 0; i < parts.size(); i++) indices.add(i);
        } catch (Exception e) {
            logger.warn("答案要点结构无法解析: answerId={}", answer.getId(), e);
        }
        return indices;
    }

    private List<Integer> matchingPartIndices(Answer answer, String normalizedInput,
                                              boolean ignorePunctuation, boolean ignoreSpaces,
                                              boolean ignoreCase) {
        List<Integer> indices = new ArrayList<>();
        try {
            JsonNode parts = objectMapper.readTree(answer.getPartsJson());
            for (int i = 0; i < parts.size(); i++) {
                StringBuilder required = new StringBuilder();
                for (JsonNode segment : parts.get(i).path("segments")) {
                    if ("required".equals(segment.path("kind").asText())) {
                        required.append(segment.path("text").asText());
                    }
                }
                if (required.length() > 0 && normalizedInput.equals(normalizeContent(required.toString(),
                        ignorePunctuation, ignoreSpaces, ignoreCase))) {
                    indices.add(i);
                }
            }
        } catch (Exception e) {
            logger.warn("答案要点结构无法解析: answerId={}", answer.getId(), e);
        }
        return indices;
    }

    private String requiredContent(Answer answer) {
        StringBuilder required = new StringBuilder();
        try {
            JsonNode parts = objectMapper.readTree(answer.getPartsJson());
            for (JsonNode part : parts) {
                for (JsonNode segment : part.path("segments")) {
                    if ("required".equals(segment.path("kind").asText())) {
                        required.append(segment.path("text").asText());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("答案要点结构无法解析: answerId={}", answer.getId(), e);
        }
        return required.toString();
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

    public List<Answer> findAnswersByContent(String content, Long userId) {
        if (content == null || content.trim().isEmpty() || userId == null) {
            return List.of();
        }
        String normalizedContent = normalizeContent(content, true, true, true);
        return answerRepository.findByNormalizedContentAndUserId(normalizedContent, userId);
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
        String result = content;
        if (ignorePunctuation) {
            result = result.replaceAll("[\\p{P}\\p{S}]", "");
        }
        if (ignoreSpaces) {
            result = result.replace("\uFEFF", "").codePoints()
                    .filter(codePoint -> !Character.isWhitespace(codePoint)
                            && !Character.isSpaceChar(codePoint))
                    .collect(StringBuilder::new,
                            StringBuilder::appendCodePoint,
                            StringBuilder::append)
                    .toString();
        }
        return ignoreCase ? result.toLowerCase(java.util.Locale.ROOT) : result;
    }
}
