package com.typingquiz.repository;

import com.typingquiz.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 答案数据访问接口
 * 提供基础CRUD操作和自定义查询方法,支持按测验和内容检索
 */
@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /**
     * 根据测验ID查询所有答案
     * @param quizId 测验ID
     * @return 该测验的所有答案列表
     */
    List<Answer> findByQuizId(Long quizId);

    /**
     * 根据标准化内容查询答案
     * @param normalizedContent 标准化后的内容(小写)
     * @return 匹配的答案列表
     */
    List<Answer> findByNormalizedContent(String normalizedContent);

    /**
     * 根据内容关键字查询答案(忽略大小写)
     * @param keyword 内容关键字
     * @return 包含关键字的答案列表
     */
    List<Answer> findByContentContainingIgnoreCase(String keyword);

    /**
     * 根据测验ID和标准化内容查询答案(取第一个匹配项)
     * @param quizId 测验ID
     * @param normalizedContent 标准化后的内容(小写)
     * @return 匹配的答案(如果存在)
     */
    Optional<Answer> findFirstByQuizIdAndNormalizedContent(Long quizId, String normalizedContent);
}
