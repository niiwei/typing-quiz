package com.typingquiz.service;

import com.typingquiz.dto.QuizGroupDTO;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.QuizGroup;
import com.typingquiz.repository.QuizGroupRepository;
import com.typingquiz.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测验分组服务类
 */
@Service
@Transactional
public class QuizGroupService {

    private final QuizGroupRepository groupRepository;
    private final QuizRepository quizRepository;

    @Autowired
    public QuizGroupService(QuizGroupRepository groupRepository, QuizRepository quizRepository) {
        this.groupRepository = groupRepository;
        this.quizRepository = quizRepository;
    }

    /**
     * 创建分组
     */
    public QuizGroup createGroup(QuizGroupDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }

        QuizGroup group = new QuizGroup(dto.getName(), dto.getDescription());
        if (dto.getDisplayOrder() != null) {
            group.setDisplayOrder(dto.getDisplayOrder());
        }

        // 添加关联的测验
        if (dto.getQuizIds() != null) {
            for (Long quizId : dto.getQuizIds()) {
                quizRepository.findById(quizId).ifPresent(group::addQuiz);
            }
        }

        return groupRepository.save(group);
    }

    /**
     * 获取所有分组
     */
    public List<QuizGroup> getAllGroups() {
        return groupRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * 根据ID获取分组
     */
    public QuizGroup getGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分组不存在: ID=" + id));
    }

    /**
     * 更新分组
     */
    public QuizGroup updateGroup(Long id, QuizGroupDTO dto) {
        QuizGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分组不存在: ID=" + id));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            group.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            group.setDescription(dto.getDescription());
        }
        if (dto.getDisplayOrder() != null) {
            group.setDisplayOrder(dto.getDisplayOrder());
        }

        // 更新关联的测验
        if (dto.getQuizIds() != null) {
            group.getQuizzes().clear();
            for (Long quizId : dto.getQuizIds()) {
                quizRepository.findById(quizId).ifPresent(group::addQuiz);
            }
        }

        return groupRepository.save(group);
    }

    /**
     * 删除分组
     */
    public void deleteGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new RuntimeException("分组不存在: ID=" + id);
        }
        groupRepository.deleteById(id);
    }

    /**
     * 向分组添加测验
     */
    public QuizGroup addQuizToGroup(Long groupId, Long quizId) {
        QuizGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("分组不存在: ID=" + groupId));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + quizId));
        group.addQuiz(quiz);
        return groupRepository.save(group);
    }

    /**
     * 从分组移除测验
     */
    public QuizGroup removeQuizFromGroup(Long groupId, Long quizId) {
        QuizGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("分组不存在: ID=" + groupId));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("测验不存在: ID=" + quizId));
        group.removeQuiz(quiz);
        return groupRepository.save(group);
    }

    /**
     * 将分组实体转换为DTO
     */
    public QuizGroupDTO toDTO(QuizGroup group) {
        QuizGroupDTO dto = new QuizGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setDisplayOrder(group.getDisplayOrder());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setQuizIds(group.getQuizzes().stream()
                .map(Quiz::getId)
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * 将分组实体列表转换为DTO列表
     */
    public List<QuizGroupDTO> toDTOList(List<QuizGroup> groups) {
        return groups.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
