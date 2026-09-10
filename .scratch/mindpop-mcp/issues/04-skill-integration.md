# 04 — 接通题库整理 Skill 并完成本地验收

**What to build:** 用户明确要求“生成并导入敲脑壳”时，mindpop-quiz-builder 会在确认和校验后调用 MCP 导入，并回读核对结果；只要求生成时不修改网站。

**Blocked by:** 03 — 提供 Codex 可调用的 stdio MCP Server

**Status:** completed

- [x] 保留 Skill 现有的材料理解、完整预览、用户确认、JSON 文件生成和确定性校验流程。
- [x] 只有明确导入意图才调用 import_quizzes；普通生成请求不触发网站写入。
- [x] 每次逻辑导入生成一个 UUID requestId，网络重试复用它，新的主动导入生成新值。
- [x] 导入成功后要求按返回 ID 调用 get_quiz，并核对题库数量、答案数量和分组。
- [x] 导入失败时保留已验证 JSON，报告稳定错误和可恢复步骤，不改用浏览器自动点击。
- [x] Skill 入口保持简洁，更新通过 skill-creator 结构校验。
- [x] Codex 本地隔离应用端到端已完成：导入、回读、版本修改、分组关联、两步删组并确认题库保留、两步删题库；临时 MySQL 与用户已清理。
- [x] 网页静态回归 9/9、Java 集成测试和 MCP 测试通过。
- [x] Standards 与 Spec 双轴代码审查已执行；未发现新增硬性规范违规，规格审查发现项已修复。
