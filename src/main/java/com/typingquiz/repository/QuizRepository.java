package com.typingquiz.repository;

import com.typingquiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测验数据访问接口
 * 提供基础CRUD操作和自定义查询方法
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * 根据标题关键字查询测验
     * @param keyword 标题关键字
     * @return 包含关键字的测验列表
     */
    List<Quiz> findByTitleContaining(String keyword);

    /**
     * 根据标题关键字查询测验(忽略大小写)
     * @param keyword 标题关键字
     * @return 包含关键字的测验列表
     */
    List<Quiz> findByTitleContainingIgnoreCase(String keyword);
}
