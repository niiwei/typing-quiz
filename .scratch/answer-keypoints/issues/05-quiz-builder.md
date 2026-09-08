# 05 — 让题库 Skill 生成可直接使用的关键词题

Status: complete

**What to build:** 文章整理结果以最小充分答案和免输入正文输出，校验后可导入实际产品并作答。

**Blocked by:** 04

## Acceptance criteria

- [x] 遵守 Skill 自身 AGENTS，保持预览等待确认流程，预览包含完整正文、必答要点、解释。
- [x] 更新生成契约和确定性校验器以支持 v2；保留旧 JSON 校验能力，允许重复要点，不生成填空题。
- [x] 约束否定、方向、关系不能删成名词；同义词不自行推断；独立知识优先分行。
- [x] 六个原始示例和降低成本/提高效率生成实际 JSON，校验并在隔离产品导入—作答—导出再导入验证。
- [x] 全局 Skill 在仓库外：保存受版本管理的修改补丁及修改前后 SHA-256 作为交付证据；不复制私密文章，不把全局目录整体纳入仓库。
- [x] 运行前四票完整回归及 Skill 有效/无效样例校验；不部署、不推送。

## Execution boundary

执行本票前读取上级规格及工程规则；仅处理本票相关模块和验证。首次执行先确认用户已放行、依赖票已验收、工作区干净。以本票行为完成为提交边界，不开始下一票、不推送、不部署。

## Evidence

提交时补充修改模块、复现失败及修复后通过的命令/结果、兼容验证和剩余风险。截图不能替代 API/数据验证，纯 helper 测试不能替代实际页面消费验证。

## Comments

实现证据：全局 skill 更新、v2 有效/无效样例、修改前后 SHA-256 及补丁见同目录 `skill-evidence.md`、`skill-update.patch`。产品链路验证见 `QuizBaselineIntegrationTest.importsAnswersFromSkillSampleAnswersExportsAndReimports`。

Execution: complete
