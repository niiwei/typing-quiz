package com.typingquiz.repository;

import com.typingquiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 获取所有测验及其答案（解决懒加载问题）
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.answers")
    List<Quiz> findAllWithAnswers();

    /**
     * 根据ID获取测验及其答案（解决懒加载问题）
     */
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.answers WHERE q.id = :id")
    Optional<Quiz> findByIdWithAnswers(@Param("id") Long id);

    /**
     * 根据用户ID查询所有测验
     */
    List<Quiz> findByUserId(Long userId);

    /**
     * 根据用户ID查询测验及其答案
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.answers WHERE q.userId = :userId")
    List<Quiz> findByUserIdWithAnswers(@Param("userId") Long userId);
}
