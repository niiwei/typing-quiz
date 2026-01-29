package com.typingquiz.controller;

import com.typingquiz.dto.QuizGroupDTO;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.service.QuizGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 获取所有分组
     */
    @GetMapping
    public List<QuizGroupDTO> getAllGroups() {
        return groupService.toDTOList(groupService.getAllGroups());
    }

    /**
     * 获取单个分组
     */
    @GetMapping("/{id}")
    public QuizGroupDTO getGroup(@PathVariable Long id) {
        return groupService.toDTO(groupService.getGroupById(id));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public QuizGroupDTO createGroup(@RequestBody QuizGroupDTO dto) {
        return groupService.toDTO(groupService.createGroup(dto));
    }

    /**
     * 更新分组
     */
    @PutMapping("/{id}")
    public QuizGroupDTO updateGroup(@PathVariable Long id, @RequestBody QuizGroupDTO dto) {
        return groupService.toDTO(groupService.updateGroup(id, dto));
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 向分组添加测验
     */
    @PostMapping("/{groupId}/quizzes/{quizId}")
    public QuizGroupDTO addQuizToGroup(@PathVariable Long groupId, @PathVariable Long quizId) {
        return groupService.toDTO(groupService.addQuizToGroup(groupId, quizId));
    }

    /**
     * 从分组移除测验
     */
    @DeleteMapping("/{groupId}/quizzes/{quizId}")
    public QuizGroupDTO removeQuizFromGroup(@PathVariable Long groupId, @PathVariable Long quizId) {
        return groupService.toDTO(groupService.removeQuizFromGroup(groupId, quizId));
    }
}
