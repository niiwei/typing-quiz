package com.typingquiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.entity.FillBlankQuiz;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizType;
import com.typingquiz.repository.FillBlankQuizRepository;
import com.typingquiz.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 填空题服务类
 */
@Service
@Transactional
public class FillBlankQuizService {

    private final FillBlankQuizRepository fillBlankQuizRepository;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FillBlankQuizService(FillBlankQuizRepository fillBlankQuizRepository, 
                                  QuizRepository quizRepository,
                                  ObjectMapper objectMapper) {
        this.fillBlankQuizRepository = fillBlankQuizRepository;
        this.quizRepository = quizRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建填空题
     */
    public FillBlankQuiz createFillBlankQuiz(Long quizId, FillBlankQuizDTO dto) {
        // 验证填空数据（测验存在性和类型已在主流程验证）
        if (dto.getFullText() == null || dto.getFullText().trim().isEmpty()) {
            throw new IllegalArgumentException("完整文本不能为空");
        }
        if (dto.getBlanks() == null || dto.getBlanks().isEmpty()) {
            throw new IllegalArgumentException("请至少设置一个填空");
        }
        
        // 生成显示文本（用___替换挖空部分）
        String displayText = generateDisplayText(dto.getFullText(), dto.getBlanks());
        
        // 序列化 blanksInfo
        String blanksInfo;
        try {
            blanksInfo = objectMapper.writeValueAsString(dto.getBlanks());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化填空信息失败");
        }
        
        // 创建填空题实体
        FillBlankQuiz fillBlankQuiz = new FillBlankQuiz(
                quizId,
                dto.getFullText(),
                blanksInfo,
                displayText,
                dto.getBlanks().size()
        );
        
        return fillBlankQuizRepository.save(fillBlankQuiz);
    }

    /**
     * 根据测验ID获取填空题
     */
    public FillBlankQuiz getByQuizId(Long quizId) {
        return fillBlankQuizRepository.findByQuizId(quizId)
                .orElseThrow(() -> new RuntimeException("填空题不存在: QuizID=" + quizId));
    }

    /**
     * 根据测验ID获取填空题DTO
     */
    public FillBlankQuizDTO getDTOByQuizId(Long quizId) {
        FillBlankQuiz entity = getByQuizId(quizId);
        return toDTO(entity);
    }

    /**
     * 更新填空题
     */
    public FillBlankQuiz updateFillBlankQuiz(Long quizId, FillBlankQuizDTO dto) {
        FillBlankQuiz existing = fillBlankQuizRepository.findByQuizId(quizId)
                .orElseThrow(() -> new RuntimeException("填空题不存在: QuizID=" + quizId));
        
        // 验证填空数据
        if (dto.getFullText() == null || dto.getFullText().trim().isEmpty()) {
            throw new IllegalArgumentException("完整文本不能为空");
        }
        if (dto.getBlanks() == null || dto.getBlanks().isEmpty()) {
            throw new IllegalArgumentException("请至少设置一个填空");
        }
        
        // 生成显示文本
        String displayText = generateDisplayText(dto.getFullText(), dto.getBlanks());
        
        // 序列化 blanksInfo
        String blanksInfo;
        try {
            blanksInfo = objectMapper.writeValueAsString(dto.getBlanks());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化填空信息失败");
        }
        
        existing.setFullText(dto.getFullText());
        existing.setBlanksInfo(blanksInfo);
        existing.setDisplayText(displayText);
        existing.setBlanksCount(dto.getBlanks().size());
        
        return fillBlankQuizRepository.save(existing);
    }

    /**
     * 删除填空题
     */
    public void deleteByQuizId(Long quizId) {
        if (!fillBlankQuizRepository.existsByQuizId(quizId)) {
            throw new RuntimeException("填空题不存在: QuizID=" + quizId);
        }
        fillBlankQuizRepository.deleteByQuizId(quizId);
    }

    /**
     * 生成显示文本（用___替换挖空部分）
     */
    private String generateDisplayText(String fullText, List<FillBlankQuizDTO.BlankInfo> blanks) {
        // 按起始位置排序
        List<FillBlankQuizDTO.BlankInfo> sortedBlanks = blanks.stream()
                .sorted((a, b) -> a.getStartIndex() - b.getStartIndex())
                .collect(Collectors.toList());
        
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        
        for (FillBlankQuizDTO.BlankInfo blank : sortedBlanks) {
            // 添加挖空前的文本
            if (blank.getStartIndex() > lastIndex) {
                result.append(fullText, lastIndex, blank.getStartIndex());
            }
            // 添加占位符
            result.append("___");
            lastIndex = blank.getEndIndex();
        }
        
        // 添加剩余文本
        if (lastIndex < fullText.length()) {
            result.append(fullText, lastIndex, fullText.length());
        }
        
        return result.toString();
    }

    /**
     * 将实体转换为DTO
     */
    public FillBlankQuizDTO toDTO(FillBlankQuiz entity) {
        FillBlankQuizDTO dto = new FillBlankQuizDTO();
        dto.setId(entity.getId());
        dto.setQuizId(entity.getQuizId());
        dto.setDisplayText(entity.getDisplayText());
        dto.setBlanksCount(entity.getBlanksCount());
        
        // 反序列化 blanksInfo
        try {
            List<FillBlankQuizDTO.BlankInfo> blanks = objectMapper.readValue(
                    entity.getBlanksInfo(), 
                    new TypeReference<List<FillBlankQuizDTO.BlankInfo>>() {}
            );
            dto.setBlanks(blanks);
            
            // 重要：将 fullText 还原为不含 [xxx] 标记的纯文本
            // 因为前端 renderFillBlankQuiz 是根据 blanks 里的 startIndex/endIndex 在 fullText 上插入标签的
            // 如果 fullText 包含了标记，索引就会对不上
            String purePlainText = entity.getFullText().replaceAll("\\[[^\\]]*\\]", "___PLANK___");
            // 这里我们需要还原出真正的纯文本（即创建时 parseFillBlankText 处理前的原始输入去标记后的样子）
            // 由于数据库中存储的是带标记的 fullText，最可靠的方法是按顺序用答案替换标记位
            String[] segments = purePlainText.split("___PLANK___", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                sb.append(segments[i]);
                if (i < blanks.size()) {
                    sb.append(blanks.get(i).getCorrectAnswer());
                }
            }
            dto.setFullText(sb.toString());
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化填空信息失败");
        }
        
        return dto;
    }
}
