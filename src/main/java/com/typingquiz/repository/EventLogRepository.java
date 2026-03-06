package com.typingquiz.repository;

import com.typingquiz.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件日志数据访问接口
 */
@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    /**
     * 根据用户ID查询事件日志
     */
    List<EventLog> findByUserId(Long userId);

    /**
     * 根据事件类型查询
     */
    List<EventLog> findByEventType(String eventType);

    /**
     * 根据用户ID和时间范围查询事件
     */
    @Query("SELECT e FROM EventLog e WHERE e.userId = :userId AND e.createdAt BETWEEN :startTime AND :endTime ORDER BY e.createdAt DESC")
    List<EventLog> findByUserIdAndTimeRange(@Param("userId") Long userId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户在指定时间内的事件数量
     */
    @Query("SELECT COUNT(e) FROM EventLog e WHERE e.userId = :userId AND e.eventType = :eventType AND e.createdAt >= :since")
    Long countByUserIdAndEventTypeSince(@Param("userId") Long userId,
                                        @Param("eventType") String eventType,
                                        @Param("since") LocalDateTime since);
}
