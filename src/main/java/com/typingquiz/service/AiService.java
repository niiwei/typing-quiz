package com.typingquiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.entity.QuizType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${llm.api_key}")
    private String apiKey;

    @Value("${llm.api_base}")
    private String apiBase;

    @Value("${llm.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public AiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(300000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public FillBlankQuizDTO generateFillBlank(String noteContent, String bookName) {
        String prompt = buildPrompt(noteContent);
        String llmResponse = callLlm(prompt);
        return parseLlmResponse(llmResponse, noteContent, bookName);
    }

    public List<QuizDTO> generateFillBlankBatch(String noteText) {
        return generateFillBlankBatchWithProgress(noteText, null);
    }

    public List<QuizDTO> generateFillBlankBatchWithProgress(String noteText, Consumer<ProgressInfo> progressCallback) {
        List<ParsedNote> parsedNotes = parseWechatReadingNotes(noteText);
        int total = parsedNotes.size();
        int completed = 0;
        
        if (progressCallback != null) {
            progressCallback.accept(new ProgressInfo(completed, total, "开始生成...", null));
        }
        
        List<CompletableFuture<QuizDTO>> futures = new ArrayList<>();
        
        for (ParsedNote note : parsedNotes) {
            CompletableFuture<QuizDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    FillBlankQuizDTO fillBlank = generateFillBlank(note.content, note.bookName);
                    QuizDTO quiz = new QuizDTO();
                    quiz.setTitle(fillBlank.getTitle());
                    quiz.setQuizType(QuizType.FILL_BLANK);
                    quiz.setFillBlankQuiz(fillBlank);
                    quiz.setGroups(note.bookName != null && !note.bookName.isEmpty() 
                        ? List.of(note.bookName) 
                        : new ArrayList<>());
                    return quiz;
                } catch (Exception e) {
                    logger.warn("生成填空题失败: {}, 跳过此条", e.getMessage());
                    return null;
                }
            }, executorService);
            futures.add(future);
        }
        
        List<QuizDTO> quizzes = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                QuizDTO quiz = futures.get(i).join();
                completed++;
                if (quiz != null && quiz.getFillBlankQuiz() != null && !quiz.getFillBlankQuiz().getBlanks().isEmpty()) {
                    quizzes.add(quiz);
                }
                if (progressCallback != null) {
                    progressCallback.accept(new ProgressInfo(completed, total, "已完成 " + completed + "/" + total, quizzes.size()));
                }
            } catch (Exception e) {
                completed++;
                logger.warn("处理第{}条笔记失败: {}", i + 1, e.getMessage());
            }
        }
        
        if (progressCallback != null) {
            progressCallback.accept(new ProgressInfo(total, total, "生成完成", quizzes.size()));
        }
        
        return quizzes;
    }

    public static class ProgressInfo {
        private final int completed;
        private final int total;
        private final String message;
        private final Integer successCount;
        
        public ProgressInfo(int completed, int total, String message, Integer successCount) {
            this.completed = completed;
            this.total = total;
            this.message = message;
            this.successCount = successCount;
        }
        
        public int getCompleted() { return completed; }
        public int getTotal() { return total; }
        public String getMessage() { return message; }
        public Integer getSuccessCount() { return successCount; }
    }

    private List<ParsedNote> parseWechatReadingNotes(String inputText) {
        List<ParsedNote> result = new ArrayList<>();
        if (inputText == null || inputText.trim().isEmpty()) {
            return result;
        }
        
        String[] lines = inputText.trim().split("\n");
        
        String bookName = "";
        if (lines.length > 0) {
            bookName = lines[0].trim().replace("《", "").replace("》", "");
        }
        
        List<String> currentNoteLines = new ArrayList<>();
        boolean collecting = false;
        int emptyLineCount = 0;
        
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            String stripped = line.trim();
            
            if (stripped.isEmpty()) {
                emptyLineCount++;
                continue;
            }
            
            if (emptyLineCount >= 2) {
                if (!currentNoteLines.isEmpty()) {
                    result.add(new ParsedNote(bookName, String.join("\n", currentNoteLines)));
                    currentNoteLines = new ArrayList<>();
                }
                collecting = false;
            }
            
            emptyLineCount = 0;
            
            if (stripped.startsWith("◆ ")) {
                if (!currentNoteLines.isEmpty()) {
                    result.add(new ParsedNote(bookName, String.join("\n", currentNoteLines)));
                    currentNoteLines = new ArrayList<>();
                }
                currentNoteLines.add(stripped.substring(2).trim());
                collecting = true;
            } else if (collecting) {
                currentNoteLines.add(stripped);
            }
        }
        
        if (!currentNoteLines.isEmpty()) {
            result.add(new ParsedNote(bookName, String.join("\n", currentNoteLines)));
        }
        
        return result;
    }

    private static class ParsedNote {
        String bookName;
        String content;
        
        ParsedNote(String bookName, String content) {
            this.bookName = bookName;
            this.content = content;
        }
    }

    private String buildPrompt(String noteContent) {
        return "你是一个教育内容生成助手。\n\n" +
            "任务：分析以下笔记，提取标题和2个核心关键词。\n\n" +
            "笔记内容：\n" + noteContent + "\n\n" +
            "要求：\n" +
            "1. 标题：简洁准确，不超过20字\n" +
            "2. 关键词：从笔记中提取2个核心概念词\n" +
            "3. 必须返回JSON格式：\n" +
            "{\"title\": \"标题\", \"keywords\": [\"关键词1\", \"关键词2\"]}\n\n" +
            "严格遵循上述格式。";
    }

    private String callLlm(String prompt) {
        String url = apiBase + "/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 1.0
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("LLM API 返回异常: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("调用 LLM 失败: {}", e.getMessage());
            throw new RuntimeException("调用 LLM 失败: " + e.getMessage());
        }
    }

    private FillBlankQuizDTO parseLlmResponse(String llmResponse, String noteContent, String bookName) {
        try {
            JsonNode root = objectMapper.readTree(llmResponse);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("LLM 响应格式错误：缺少 choices");
            }

            String content = choices.get(0).get("message").get("content").asText();
            
            int jsonStart = content.indexOf('{');
            int jsonEnd = content.lastIndexOf('}');
            if (jsonStart == -1 || jsonEnd == -1) {
                throw new RuntimeException("LLM 响应中未找到 JSON");
            }

            String jsonStr = content.substring(jsonStart, jsonEnd + 1);
            JsonNode jsonData = objectMapper.readTree(jsonStr);

            String title = jsonData.get("title").asText();
            JsonNode keywordsNode = jsonData.get("keywords");
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.size() < 2) {
                throw new RuntimeException("LLM 响应缺少 keywords 或 keywords 少于2个");
            }

            List<String> keywords = new ArrayList<>();
            for (JsonNode keyword : keywordsNode) {
                keywords.add(keyword.asText());
            }

            return buildFillBlankQuiz(title, noteContent, keywords, bookName);

        } catch (Exception e) {
            logger.error("解析 LLM 响应失败: {}", e.getMessage());
            throw new RuntimeException("解析 LLM 响应失败: " + e.getMessage());
        }
    }

    private FillBlankQuizDTO buildFillBlankQuiz(String title, String fullText, List<String> keywords, String bookName) {
        List<FillBlankQuizDTO.BlankInfo> blanks = new ArrayList<>();
        String displayText = fullText;
        int offset = 0;

        for (String keyword : keywords) {
            int idx = displayText.indexOf(keyword);
            if (idx != -1) {
                int startIndex = idx + offset;
                int endIndex = startIndex + keyword.length();
                
                blanks.add(new FillBlankQuizDTO.BlankInfo(startIndex, endIndex, keyword));
                
                displayText = displayText.substring(0, idx) + "____" + displayText.substring(idx + keyword.length());
                offset += 4 - keyword.length();
            }
        }

        FillBlankQuizDTO dto = new FillBlankQuizDTO();
        dto.setTitle(title);
        dto.setFullText(fullText);
        dto.setDisplayText(displayText);
        dto.setBlanks(blanks);
        dto.setBlanksCount(blanks.size());

        return dto;
    }
}
