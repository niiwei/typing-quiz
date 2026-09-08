INSERT INTO quiz_group (user_id, name, description, display_order, created_at) SELECT DISTINCT user_id, '默认分组', '系统自动创建的默认分组', 0, NOW() FROM quiz WHERE user_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM quiz_group g WHERE g.user_id = quiz.user_id AND g.name = '默认分组');

INSERT INTO quiz_group_quiz (group_id, quiz_id) SELECT dg.id, q.id FROM quiz q INNER JOIN quiz_group dg ON dg.user_id = q.user_id AND dg.name = '默认分组' WHERE NOT EXISTS (SELECT 1 FROM quiz_group_quiz qgq WHERE qgq.quiz_id = q.id);
