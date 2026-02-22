package com.typingquiz.controller;

import com.typingquiz.dto.QuizGroupDTO;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.service.QuizGroupService;
import com.typingquiz.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 测验分组控制器
 */
@RestController
@RequestMapping("/api/groups")
public class QuizGroupController {

    private final QuizGroupService groupService;

    @Autowired
    public QuizGroupController(QuizGroupService groupService) {
        this.groupService = groupService;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (JwtUtil.validateToken(token)) {
                return JwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }

    /**
     * 获取所有分组
     */
    @GetMapping
    public List<QuizGroupDTO> getAllGroups(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return groupService.toDTOList(groupService.getAllGroups(userId));
    }

    /**
     * 获取单个分组
     */
    @GetMapping("/{id}")
    public QuizGroupDTO getGroup(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        QuizGroup group = groupService.getGroupById(id);
        // 验证用户身份
        if (userId != null && !userId.equals(group.getUserId())) {
            throw new RuntimeException("无权访问此分组");
        }
        return groupService.toDTO(group);
    }

    /**
     * 创建分组
     */
    @PostMapping
    public QuizGroupDTO createGroup(@RequestBody QuizGroupDTO dto, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return groupService.toDTO(groupService.createGroup(dto, userId));
    }

    /**
     * 更新分组
     */
    @PutMapping("/{id}")
    public QuizGroupDTO updateGroup(@PathVariable Long id, @RequestBody QuizGroupDTO dto, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return groupService.toDTO(groupService.updateGroup(id, dto, userId));
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        groupService.deleteGroup(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 向分组添加测验
     */
    @PostMapping("/{groupId}/quizzes/{quizId}")
    public QuizGroupDTO addQuizToGroup(@PathVariable Long groupId, @PathVariable Long quizId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return groupService.toDTO(groupService.addQuizToGroup(groupId, quizId, userId));
    }

    /**
     * 从分组移除测验
     */
    @DeleteMapping("/{groupId}/quizzes/{quizId}")
    public QuizGroupDTO removeQuizFromGroup(@PathVariable Long groupId, @PathVariable Long quizId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return groupService.toDTO(groupService.removeQuizFromGroup(groupId, quizId, userId));
    }

    /**
     * 获取分组内的所有测验（用于分组答题）
     * GET /api/groups/{groupId}/quizzes
     * 支持特殊值 "ungrouped" 获取未分组的测验
     */
    @GetMapping("/{groupId}/quizzes")
    public ResponseEntity<List<com.typingquiz.dto.QuizResponseDTO>> getGroupQuizzes(@PathVariable String groupId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            
            // 处理未分组虚拟分组
            if ("ungrouped".equals(groupId)) {
                return getUngroupedQuizzes(userId);
            }
            
            // 普通分组处理
            Long id = Long.parseLong(groupId);
            QuizGroup group = groupService.getGroupByIdWithQuizzes(id);
            // 验证用户身份
            if (userId != null && !userId.equals(group.getUserId())) {
                throw new RuntimeException("无权访问此分组");
            }
            
            // 批量查询答案数量，避免 N+1
            List<Long> quizIds = group.getQuizzes().stream()
                    .map(com.typingquiz.entity.Quiz::getId)
                    .collect(java.util.stream.Collectors.toList());
            
            Map<Long, Integer> answerCountMap = new java.util.HashMap<>();
            if (!quizIds.isEmpty()) {
                List<Object[]> answerCounts = groupService.getAnswerCountsByQuizIds(quizIds);
                for (Object[] row : answerCounts) {
                    answerCountMap.put((Long) row[0], ((Long) row[1]).intValue());
                }
            }
            
            final Map<Long, Integer> finalCountMap = answerCountMap;
            List<com.typingquiz.dto.QuizResponseDTO> quizzes = group.getQuizzes().stream()
                    .map(quiz -> {
                        com.typingquiz.dto.QuizResponseDTO dto = new com.typingquiz.dto.QuizResponseDTO();
                        dto.setId(quiz.getId());
                        dto.setTitle(quiz.getTitle());
                        dto.setDescription(quiz.getDescription());
                        dto.setTimeLimit(quiz.getTimeLimit());
                        dto.setQuizType(quiz.getQuizType());
                        dto.setTotalAnswers(finalCountMap.getOrDefault(quiz.getId(), 0));
                        return dto;
                    })
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(quizzes);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取未分组的测验列表
     */
    private ResponseEntity<List<com.typingquiz.dto.QuizResponseDTO>> getUngroupedQuizzes(Long userId) {
        // 获取用户的所有测验
        List<com.typingquiz.entity.Quiz> allQuizzes = groupService.getAllQuizzesByUserId(userId);
        
        // 获取用户的所有分组
        List<QuizGroup> groups = groupService.getAllGroups(userId);
        
        // 找出已分组的测验ID
        java.util.Set<Long> groupedQuizIds = new java.util.HashSet<>();
        for (QuizGroup group : groups) {
            if (group.getQuizzes() != null) {
                for (com.typingquiz.entity.Quiz quiz : group.getQuizzes()) {
                    groupedQuizIds.add(quiz.getId());
                }
            }
        }
        
        // 过滤出未分组的测验
        List<com.typingquiz.entity.Quiz> ungroupedQuizzes = allQuizzes.stream()
                .filter(quiz -> !groupedQuizIds.contains(quiz.getId()))
                .collect(java.util.stream.Collectors.toList());
        
        // 查询答案数量
        List<Long> quizIds = ungroupedQuizzes.stream()
                .map(com.typingquiz.entity.Quiz::getId)
                .collect(java.util.stream.Collectors.toList());
        
        Map<Long, Integer> answerCountMap = new java.util.HashMap<>();
        if (!quizIds.isEmpty()) {
            List<Object[]> answerCounts = groupService.getAnswerCountsByQuizIds(quizIds);
            for (Object[] row : answerCounts) {
                answerCountMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
        }
        
        // 转换为DTO
        List<com.typingquiz.dto.QuizResponseDTO> result = ungroupedQuizzes.stream()
                .map(quiz -> {
                    com.typingquiz.dto.QuizResponseDTO dto = new com.typingquiz.dto.QuizResponseDTO();
                    dto.setId(quiz.getId());
                    dto.setTitle(quiz.getTitle());
                    dto.setDescription(quiz.getDescription());
                    dto.setTimeLimit(quiz.getTimeLimit());
                    dto.setQuizType(quiz.getQuizType());
                    dto.setTotalAnswers(answerCountMap.getOrDefault(quiz.getId(), 0));
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}
