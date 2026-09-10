# MindPop 项目管理规范

本项目采用一条稳定主线和短生命周期功能分支，目标是让需求、代码、GitHub 与生产版本能够互相追溯。

## 分支与任务

- `main` 始终保持可发布，并作为生产环境的唯一来源。
- 每项工作从最新 `main` 创建 `codex/<topic>` 分支。
- 多步骤需求先写 `.scratch/<feature>/spec.md`，再拆成 `issues/NN-slug.md`；每张 ticket 标明依赖、范围和验收标准。
- 一次提交只表达一个目的，使用 `feat`、`fix`、`docs`、`refactor`、`perf` 或 `chore`。

## 合并门槛

合并前必须满足：

1. 需求范围和验收标准已经完成。
2. Java 测试、MCP 测试以及与改动有关的浏览器测试通过。
3. `git diff --check` 无错误，文档与配置示例同步。
4. 凭据、构建产物、数据库和本机配置未进入 Git。
5. 审查发现的行为、安全和数据隔离问题已经关闭。

简单改动可以 squash 合并；需要保留多个独立决策时使用普通合并。合并后删除功能分支属于清理动作，不影响历史追溯。

## GitHub 与 CI

功能分支先推送到 GitHub，再合并 `main`。`.github/workflows/ci.yml` 在 Pull Request 和 `main` 推送时验证 Java 与 MCP。CI 未通过时不得发布。

远端 `main` 禁止强推。出现分叉时先查清目录结构和共同祖先，通过迁移分支保留两边历史；不能用 `--force` 掩盖结构问题。

## 发布

合并并通过 CI 后，在本地最新 `main` 执行 `./scripts/deploy-main.sh`。脚本只构建已推送的提交，并把提交 SHA 写入服务器，详情见根目录 `DEPLOY.md`。

发布记录至少包含：提交 SHA、验证结果、数据库迁移状态、服务器健康检查和回滚备份位置。

## 回滚与热修

- 应用问题：恢复服务器备份 JAR，并在 Git 中创建 `git revert` 提交。
- 数据问题：停止应用后从发布前数据库备份恢复。
- 紧急修复：从 `main` 创建 `codex/hotfix-<topic>`，走同一测试、合并和发布流程。

不得使用 `git reset --hard` 加强推来回退共享 `main`。
