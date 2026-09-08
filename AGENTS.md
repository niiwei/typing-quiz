# MindPop 工作区规则

- 默认中文沟通，代码、命令和变量名使用英文。
- Git 根目录为本目录；工程入口为 `typing-quiz-main/`，不另建嵌套仓库。
- 新目录先约定用途、命名和清理规则；调整规则先改文档再改实践。
- 只修改当前任务所需文件，不顺手重构；实现前明确假设及验收标准。
- 提交前检查差异，执行与改动相关的验证；不得将未运行的验证写为通过。
- 本地提交用于版本记录；未经明确授权不推送或部署。
- 不提交密钥、真实环境配置、数据库、构建产物和临时文件。

## 目录约定

- 根目录仅存放仓库规则和 Git 配置；源码及项目文档位于 `typing-quiz-main/`。
- 工程 `src/` 存放运行代码及资源，保留既有目录结构。
- 工程 `docs/agents/` 存放工程技能配置；`CONTEXT.md` 存放领域术语；`docs/adr/` 存放编号决策记录。
- 工程 `.scratch/<feature>/` 存放可版本管理的需求和任务文档，功能名使用英文 kebab-case；临时输出不得混入。
- 需求为 `spec.md`，任务为 `issues/NN-slug.md`；完成后保留记录，不自动删除历史。
- 新增临时产物应在交付前清理，仅清理本次产生的文件。现有目录不搬迁、不批量清理。

## Agent skills

### Issue tracker

使用工程内本地 Markdown。参见 `typing-quiz-main/docs/agents/issue-tracker.md`。

### Triage labels

使用默认五个角色标签。参见 `typing-quiz-main/docs/agents/triage-labels.md`。

### Domain docs

采用 single-context，文档相对工程根目录定位。参见 `typing-quiz-main/docs/agents/domain.md`。

## 本地凭据文件

两个已有原文件含凭据并已排除 Git：工程 `src/main/resources/application.properties` 和 `wechat_notes_to_quiz/wechat_notes_to_quiz.py`。对应 `.example` 文件纳入版本管理，恢复工作区时复制为原文件名并配置 `MINIMAX_API_KEY` / `ARK_API_KEY` 环境变量。不要强制添加原文件。
