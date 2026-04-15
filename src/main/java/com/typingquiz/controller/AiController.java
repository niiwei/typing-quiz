package com.typingquiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.BatchGenerateRequest;
import com.typingquiz.dto.BatchGenerateResponse;
import com.typingquiz.dto.FillBlankQuizDTO;
import com.typingquiz.dto.GenerateFillBlankRequest;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.service.AiService;
import com.typingquiz.service.AiService.ProgressInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor;

    public AiController(AiService aiService) {
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
        this.sseExecutor = Executors.newCachedThreadPool();
    }

    @PostMapping("/generate-fill-blank")
    public ResponseEntity<?> generateFillBlank(@RequestBody GenerateFillBlankRequest request) {
        try {
            if (request.getNoteContent() == null || request.getNoteContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "笔记内容不能为空"));
            }
            
            String noteContent = request.getNoteContent().trim();
            String bookName = request.getBookName() != null ? request.getBookName().trim() : "";
            
            logger.info("生成填空题请求: bookName={}, contentLength={}", bookName, noteContent.length());
            
            FillBlankQuizDTO result = aiService.generateFillBlank(noteContent, bookName);
            
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.error("生成填空题失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("生成填空题异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "服务器内部错误"));
        }
    }

    @PostMapping(value = "/generate-fill-blank-batch-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateFillBlankBatchStream(@RequestBody BatchGenerateRequest request) {
        if (request.getNoteText() == null || request.getNoteText().trim().isEmpty()) {
            return null;
        }

        SseEmitter emitter = new SseEmitter(300000L);
        
        sseExecutor.execute(() -> {
            try {
                logger.info("SSE批量生成填空题请求: contentLength={}", request.getNoteText().length());
                
                Consumer<ProgressInfo> progressCallback = progress -> {
                    try {
                        String data = objectMapper.writeValueAsString(Map.of(
                            "type", "progress",
                            "completed", progress.getCompleted(),
                            "total", progress.getTotal(),
                            "message", progress.getMessage(),
                            "successCount", progress.getSuccessCount() != null ? progress.getSuccessCount() : 0
                        ));
                        emitter.send(SseEmitter.event().name("progress").data(data));
                    } catch (Exception e) {
                        logger.warn("发送进度失败: {}", e.getMessage());
                    }
                };
                
                List<QuizDTO> quizzes = aiService.generateFillBlankBatchWithProgress(request.getNoteText(), progressCallback);
                
                String bookName = "";
                if (!quizzes.isEmpty() && quizzes.get(0).getGroups() != null && !quizzes.get(0).getGroups().isEmpty()) {
                    bookName = quizzes.get(0).getGroups().get(0);
                }
                
                logger.info("SSE批量生成完成: 生成 {} 道填空题", quizzes.size());
                
                String finalData = objectMapper.writeValueAsString(BatchGenerateResponse.success(bookName, quizzes));
                emitter.send(SseEmitter.event().name("complete").data(finalData));
                emitter.complete();
                
            } catch (Exception e) {
                logger.error("SSE批量生成失败: {}", e.getMessage());
                try {
                    String errorData = objectMapper.writeValueAsString(BatchGenerateResponse.error(e.getMessage()));
                    emitter.send(SseEmitter.event().name("error").data(errorData));
                } catch (Exception ex) {
                    logger.warn("发送错误失败: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        });
        
        emitter.onCompletion(() -> logger.info("SSE连接完成"));
        emitter.onTimeout(() -> logger.warn("SSE连接超时"));
        emitter.onError(e -> logger.warn("SSE连接错误: {}", e.getMessage()));
        
        return emitter;
    }

    @PostMapping("/generate-fill-blank-batch")
    public ResponseEntity<?> generateFillBlankBatch(@RequestBody BatchGenerateRequest request) {
        try {
            if (request.getNoteText() == null || request.getNoteText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(BatchGenerateResponse.error("笔记内容不能为空"));
            }
            
            logger.info("批量生成填空题请求: contentLength={}", request.getNoteText().length());
            
            List<QuizDTO> quizzes = aiService.generateFillBlankBatch(request.getNoteText());
            
            String bookName = "";
            if (!quizzes.isEmpty() && quizzes.get(0).getGroups() != null && !quizzes.get(0).getGroups().isEmpty()) {
                bookName = quizzes.get(0).getGroups().get(0);
            }
            
            logger.info("批量生成完成: 生成 {} 道填空题", quizzes.size());
            
            return ResponseEntity.ok(BatchGenerateResponse.success(bookName, quizzes));
        } catch (RuntimeException e) {
            logger.error("批量生成填空题失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(BatchGenerateResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("批量生成填空题异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(BatchGenerateResponse.error("服务器内部错误"));
        }
    }
}
