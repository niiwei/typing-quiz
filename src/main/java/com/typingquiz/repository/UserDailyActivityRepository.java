package com.typingquiz.repository;

import com.typingquiz.entity.UserDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 用户每日活动统计数据访问接口
 */
@Repository
public interface UserDailyActivityRepository extends JpaRepository<UserDailyActivity, Long> {

    /**
     * 根据用户ID和日期查询活动记录
     */
    Optional<UserDailyActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);

    /**
     * 查询用户在指定日期范围内的活动记录（用于热力图）
     */
    @Query("SELECT u FROM UserDailyActivity u WHERE u.userId = :userId AND u.activityDate BETWEEN :startDate AND :endDate ORDER BY u.activityDate")
    List<UserDailyActivity> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * 查询用户的所有活动记录
     */
    List<UserDailyActivity> findByUserIdOrderByActivityDateDesc(Long userId);

    /**
     * 查询用户最近的连续活跃记录（用于计算连续天数）
     */
    @Query("SELECT u FROM UserDailyActivity u WHERE u.userId = :userId AND u.isActive = true ORDER BY u.activityDate DESC")
    List<UserDailyActivity> findActiveDaysByUserId(@Param("userId") Long userId);

    /**
     * 查询指定日期的活跃用户数量（站长统计）
     */
    @Query("SELECT COUNT(DISTINCT u.userId) FROM UserDailyActivity u WHERE u.activityDate = :date AND u.isActive = true")
    Long countActiveUsersByDate(@Param("date") LocalDate date);

    /**
     * 查询用户的总学习天数
     */
    @Query("SELECT COUNT(u) FROM UserDailyActivity u WHERE u.userId = :userId AND u.isActive = true")
    Long countTotalActiveDaysByUserId(@Param("userId") Long userId);

    /**
     * 查询指定日期的所有用户活动（用于站长统计）
     */
    @Query("SELECT u FROM UserDailyActivity u WHERE u.activityDate = :date")
    List<UserDailyActivity> findByActivityDate(@Param("date") LocalDate date);
}
