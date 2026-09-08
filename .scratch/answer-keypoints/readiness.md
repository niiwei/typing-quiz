# 执行前审计

日期：2026-09-08。范围：规格、拆票及 01 首票证据。

## 当前证据

- 本地基线提交 e75f2f1，需求确认提交 1226f02；规划开始时工作区干净。
- 工程声明 Java 11 / Spring Boot 2.7.18，含 Spring Boot Test 依赖；已增加 `src/test` 基线。
- Homebrew 用户目录提供 OpenJDK 11.0.32.1；工程 `sh mvnw` 可执行。Node 和 Playwright 浏览器已安装到用户缓存。
- 01 已完成环境准备、后端旧题往返和真实页面 smoke；02–05 按依赖解锁。功能票继续使用隔离 profile，不连接线上数据库。

## 已核对代码入口（相对工程根目录）

| 行为 | 当前入口及发现 |
|---|---|
| 匹配 | src/main/resources/static/js/quiz-controller.js：normalizeText 仅 trim；checkAnswer 首次命中即返回 |
| 后端匹配 | src/main/java/com/typingquiz/service/AnswerService.java：首条 normalizedContent 查询；ValidationResponse 为单答案 |
| 持久化 | src/main/java/com/typingquiz/entity/Answer.java：content/comment/normalizedContent，生命周期仅 trim+lowercase |
| 创建导入 | src/main/java/com/typingquiz/service/QuizService.java：addedAnswers 会去重 |
| 编辑导出 | 同一 QuizService 的 updateQuiz 和 answerList DTO 映射需贯通新字段 |
| 注释编辑 | src/main/resources/static/create.html：parseTypingAnswerWithComment 仅末尾注释 |
| 卡片结算 | src/main/resources/static/js/ui-renderer.js；quiz-controller 的 endQuiz 按 foundAnswers 算分 |
| 复习 | 现有测验级复习控制器和服务；不新建答案级排程 |
| Skill | 全局 mindpop-quiz-builder/SKILL.md 与 scripts/validate_quiz_json.py；工程外修改单独留补丁证据 |

## 需求覆盖

| 确认事项 | 执行票 |
|---|---|
| 空白归一化、前后端一致 | 02 |
| 任意位置免输入正文、按钮、转义、解释分离 | 03 |
| 完整句子与关键词都通过 | 03、04 |
| 空白初始卡片、部分显现、不泄漏长度 | 03、04 |
| 多要点、逆序、同时点亮、不做唯一性校验 | 04 |
| 部分完成保持未完成并进入复习 | 04 |
| Skill 最小充分答案、预览、实际导入 | 05 |
| 旧题兼容、导入导出往返 | 01、03、04、05 |
| 测试可执行、独立数据库、不触及线上 | 01，后续每票复用 |

## 执行条件结论

规划完整，五票均有范围、依赖和可观察验收，依赖链无环。01 已有真实后端和浏览器基线证据；02 已解锁。实现票不能把规划检查当作应用测试。
