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
            
            // 重要：将 fullText 还原为不含任何 [xxx] 标记的纯文本
            // 因为前端 renderFillBlankQuiz 是根据 blanks 里的 startIndex/endIndex 在 fullText 上插入标签的
            // 如果 fullText 包含了标记，索引就会对不上
            
            String rawFullText = entity.getFullText();
            // 先将数据库中的双井号 ## 替换为单井号 #，以匹配 blanks_info 中的索引
            String normalizedText = rawFullText.replace("##", "#");
            // 使用正则匹配方括号内的所有内容，确保完整匹配 [答案#注释#] 格式
            String placeholderText = normalizedText.replaceAll("\\[[^\\]]*\\]", "___PLANK___");
            String[] segments = placeholderText.split("___PLANK___", -1);
            
            // 重建纯文本 fullText，并重新计算 blanks 的索引
            StringBuilder sb = new StringBuilder();
            int currentIndex = 0;
            List<FillBlankQuizDTO.BlankInfo> updatedBlanks = new ArrayList<>();
            
            for (int i = 0; i < blanks.size(); i++) {
                // 添加当前段落到纯文本
                if (i < segments.length) {
                    sb.append(segments[i]);
                    currentIndex += segments[i].length();
                }
                
                // 记录 blank 的新索引
                FillBlankQuizDTO.BlankInfo oldBlank = blanks.get(i);
                FillBlankQuizDTO.BlankInfo newBlank = new FillBlankQuizDTO.BlankInfo();
                newBlank.setStartIndex(currentIndex);
                newBlank.setCorrectAnswer(oldBlank.getCorrectAnswer());
                newBlank.setComment(oldBlank.getComment());
                
                // 添加答案文本到纯文本
                sb.append(oldBlank.getCorrectAnswer());
                newBlank.setEndIndex(currentIndex + oldBlank.getCorrectAnswer().length());
                currentIndex = newBlank.getEndIndex();
                
                updatedBlanks.add(newBlank);
            }
            
            // 添加最后一段（如果有）
            if (blanks.size() < segments.length) {
                sb.append(segments[segments.length - 1]);
            }
            
            dto.setFullText(sb.toString());
            dto.setBlanks(updatedBlanks);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化填空信息失败");
        }
        
        return dto;
    }
}
