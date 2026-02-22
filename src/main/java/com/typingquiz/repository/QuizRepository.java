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

    /**
     * 根据用户ID统计测验数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户ID查询测验（轻量级，不加载答案，用于列表展示）
     */
    @Query("SELECT q FROM Quiz q WHERE q.userId = :userId ORDER BY q.createdAt DESC")
    List<Quiz> findByUserIdSimple(@Param("userId") Long userId);

    /**
     * 查询用户所有测验的答案数量（单次查询，避免N+1）
     * 返回 Map: quizId -> answerCount
     */
    @Query("SELECT q.id, COUNT(a) FROM Quiz q LEFT JOIN q.answers a WHERE q.userId = :userId GROUP BY q.id")
    List<Object[]> findAnswerCountsByUserId(@Param("userId") Long userId);

    /**
     * 批量查询指定测验的答案数量（避免N+1）
     * 返回 Object[]: [quizId, answerCount]
     */
    @Query("SELECT q.id, COUNT(a) FROM Quiz q LEFT JOIN q.answers a WHERE q.id IN :quizIds GROUP BY q.id")
    List<Object[]> findAnswerCountsByQuizIds(@Param("quizIds") List<Long> quizIds);
}
