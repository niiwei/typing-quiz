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
            
            // 根据 blanks 重建 fullText（修复注释丢失问题）
            String reconstructedFullText = reconstructFullText(entity.getFullText(), blanks);
            dto.setFullText(reconstructedFullText);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化填空信息失败");
        }
        
        return dto;
    }
    
    /**
     * 根据 blanks 数组重建 fullText，确保注释格式正确
     * 修复已有数据中注释丢失导致显示为 ## 的问题
     */
    private String reconstructFullText(String originalFullText, List<FillBlankQuizDTO.BlankInfo> blanks) {
        if (blanks == null || blanks.isEmpty()) {
            return originalFullText;
        }
        
        // 按起始位置排序（降序，从后向前替换）
        List<FillBlankQuizDTO.BlankInfo> sortedBlanks = blanks.stream()
                .sorted((a, b) -> b.getStartIndex() - a.getStartIndex())
                .collect(Collectors.toList());
        
        StringBuilder result = new StringBuilder(originalFullText);
        
        for (FillBlankQuizDTO.BlankInfo blank : sortedBlanks) {
            int start = blank.getStartIndex();
            int end = blank.getEndIndex();
            String correctAnswer = blank.getCorrectAnswer();
            String comment = blank.getComment();
            
            // 重建填空格式：[答案#注释#] 或 [答案]
            String reconstructedBlank;
            if (comment != null && !comment.trim().isEmpty()) {
                reconstructedBlank = "[" + correctAnswer + "#" + comment + "#]";
            } else {
                reconstructedBlank = "[" + correctAnswer + "]";
            }
            
            // 替换原文本中的填空部分
            // 注意：这里假设 start 和 end 是答案在去除标记后的位置
            // 我们需要在 originalFullText 中找到对应的 [xxx] 并替换
            
            // 找到 start 位置前的最后一个 [
            int bracketStart = result.lastIndexOf("[", start + 1);
            int bracketEnd = result.indexOf("]", start);
            
            if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {
                result.replace(bracketStart, bracketEnd + 1, reconstructedBlank);
            }
        }
        
        return result.toString();
    }
}
