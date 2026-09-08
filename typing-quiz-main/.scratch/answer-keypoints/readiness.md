# 执行前审计

日期：2026-09-08。范围：规格和拆票准备；不声称实现或应用测试通过。

## 当前证据

- 本地基线提交 e75f2f1，需求确认提交 1226f02；规划开始时工作区干净。
- 工程声明 Java 11 / Spring Boot 2.7.18，含 Spring Boot Test 依赖，但无 src/test。
- `java -version` 返回 Unable to locate a Java Runtime；`command -v mvn` 无结果。已有 mvnw 可获取 Maven Wrapper，但仍需 JDK。Node 命令可用。
- 因此首票可开始环境准备；02–05 不可跳过首票直接宣称可运行。当前不执行应用构建、数据库初始化或线上访问。
- 用户明确要求先拆票后检验，本次将测试入口和拆票方案一并提交检验，不在编写中反复要求确认，也不启动实现。

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

规划完整，五票均有范围、依赖和可观察验收，依赖链无环。当前可放行的是 01；其余按依赖逐张解锁。实现前必须准备环境，不能把规划检查当作应用测试。停止点为用户检验本组文档。
