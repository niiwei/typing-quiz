-- 方案3数据迁移脚本：创建默认分组并迁移未分组测验
-- 执行时间: 2025-02-23

-- 步骤1: 为每个用户创建"默认分组"
INSERT INTO quiz_group (user_id, name, description, display_order, created_at)
SELECT DISTINCT user_id, '默认分组', '系统自动创建的默认分组', 0, NOW()
FROM quiz
WHERE user_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM quiz_group g 
    WHERE g.user_id = quiz.user_id AND g.name = '默认分组'
);

-- 步骤2: 将未分组的测验关联到默认分组
-- 注意: 使用中间表 quiz_group_quiz 关联
INSERT INTO quiz_group_quiz (group_id, quiz_id)
SELECT dg.id, q.id
FROM quiz q
INNER JOIN quiz_group dg ON dg.user_id = q.user_id AND dg.name = '默认分组'
WHERE NOT EXISTS (
    SELECT 1 FROM quiz_group_quiz qgq 
    WHERE qgq.quiz_id = q.id
);

-- 验证查询
SELECT 
    u.username,
    COUNT(DISTINCT q.id) as total_quizzes,
    COUNT(DISTINCT CASE WHEN qgq.quiz_id IS NULL THEN q.id END) as ungrouped_quizzes
FROM quiz q
JOIN user u ON q.user_id = u.id
LEFT JOIN quiz_group_quiz qgq ON q.id = qgq.quiz_id
GROUP BY u.id, u.username;
