-- 上线前先执行此只读预检。若返回任何行，停止迁移并人工处理重复分组；不自动合并。
SELECT user_id, LOWER(TRIM(name)) AS normalized_name, COUNT(*) AS duplicate_count
FROM quiz_group
GROUP BY user_id, LOWER(TRIM(name))
HAVING COUNT(*) > 1;
