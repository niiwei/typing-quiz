SET @format_version_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer' AND COLUMN_NAME = 'format_version');
SET @format_version_sql = IF(@format_version_exists = 0, 'ALTER TABLE answer ADD COLUMN format_version INT NULL', 'SELECT 1');
PREPARE format_version_stmt FROM @format_version_sql;
EXECUTE format_version_stmt;
DEALLOCATE PREPARE format_version_stmt;

SET @parts_json_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer' AND COLUMN_NAME = 'parts_json');
SET @parts_json_sql = IF(@parts_json_exists = 0, 'ALTER TABLE answer ADD COLUMN parts_json TEXT NULL', 'SELECT 1');
PREPARE parts_json_stmt FROM @parts_json_sql;
EXECUTE parts_json_stmt;
DEALLOCATE PREPARE parts_json_stmt;

INSERT INTO quiz_group (user_id, name, description, display_order, created_at) SELECT DISTINCT user_id, '默认分组', '系统自动创建的默认分组', 0, NOW() FROM quiz WHERE user_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM quiz_group g WHERE g.user_id = quiz.user_id AND g.name = '默认分组');

INSERT INTO quiz_group_quiz (group_id, quiz_id) SELECT dg.id, q.id FROM quiz q INNER JOIN quiz_group dg ON dg.user_id = q.user_id AND dg.name = '默认分组' WHERE NOT EXISTS (SELECT 1 FROM quiz_group_quiz qgq WHERE qgq.quiz_id = q.id);
