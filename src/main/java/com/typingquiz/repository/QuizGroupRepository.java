package com.typingquiz.repository;

import com.typingquiz.entity.QuizGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测验分组数据访问接口
 */
@Repository
public interface QuizGroupRepository extends JpaRepository<QuizGroup, Long> {

    /**
     * 根据名称查询分组
     */
    List<QuizGroup> findByNameContaining(String name);

    /**
     * 根据名称查询分组(忽略大小写)
     */
    List<QuizGroup> findByNameContainingIgnoreCase(String name);

    /**
     * 按排序顺序获取所有分组
     */
    List<QuizGroup> findAllByOrderByDisplayOrderAsc();
}
