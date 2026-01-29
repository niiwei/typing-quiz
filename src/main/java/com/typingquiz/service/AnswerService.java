package com.typingquiz.service;

import com.typingquiz.dto.ValidationResponse;
import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.repository.AnswerRepository;
import com.typingquiz.repository.QuizRepository;
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

    private final AnswerRepository answerRepository;
    private final QuizRepository quizRepository;

    @Autowired
    public AnswerService(AnswerRepository answerRepository, QuizRepository quizRepository) {
        this.answerRepository = answerRepository;
        this.quizRepository = quizRepository;
    }

    /**
     * 验证答案(大小写不敏感)
     * @param quizId 测验ID
     * @param input 用户输入
     * @return 验证结果
     */
    public ValidationResponse validateAnswer(Long quizId, String input) {
        // 验证输入
        if (input == null || input.trim().isEmpty()) {
            return new ValidationResponse(false, null, null, false);
        }

        // 标准化输入(转小写,去空格)
        String normalizedInput = normalizeContent(input);

        // 查询答案
        Optional<Answer> answerOpt = answerRepository
                .findByQuizIdAndNormalizedContent(quizId, normalizedInput);

        if (answerOpt.isPresent()) {
            Answer answer = answerOpt.get();
            return new ValidationResponse(
                true,
                answer.getId(),
                answer.getContent(),  // 返回原始大小写
                false  // 前端负责跟踪已找到的答案
            );
        }

        return new ValidationResponse(false, null, null, false);
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
        String normalizedContent = normalizeContent(content);
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
    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.trim().toLowerCase();
    }
}
