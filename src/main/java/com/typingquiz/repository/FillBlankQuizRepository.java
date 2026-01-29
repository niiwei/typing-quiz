package com.typingquiz.repository;

import com.typingquiz.entity.FillBlankQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 填空题数据访问接口
 */
@Repository
public interface FillBlankQuizRepository extends JpaRepository<FillBlankQuiz, Long> {

    /**
     * 根据测验ID查找填空题
     */
    Optional<FillBlankQuiz> findByQuizId(Long quizId);

    /**
     * 检查测验ID是否存在
     */
    boolean existsByQuizId(Long quizId);

    /**
     * 根据测验ID删除
     */
    void deleteByQuizId(Long quizId);
}
