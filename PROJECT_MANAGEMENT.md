# Typing Quiz 项目管理与回滚指引

本指南用于规划 Typing Quiz 项目的迭代流程，确保每次发布可追溯、可回滚，并便于团队协作。

## 1. 分支策略

- **main 分支**：只保存稳定发布版本。任何合并到 main 的代码都必须经过测试，并配套版本标签。
- **develop 分支**（可选）：作为日常集成分支，所有功能分支先合并到 develop，经验证后再合并到 main。
- **feature/xxx 分支**：每个功能或修复建立独立分支，命名为 `feature/<简述>` 或 `fix/<问题>`。

> 如暂不需要复杂流程，也可以只保留 main + feature 分支结构。

## 2. 版本号与标签

- 采用 **SemVer**（语义化版本）：`主版本.次版本.修订`（例如 `v1.2.0`）。
- 每次发布到 main 前：
  1. 更新 `pom.xml` 的 `<version>` 与相关文档版本号。
  2. 运行 `./mvnw clean package` 确认构建通过。
  3. 生成发布包（jar、zip 等），放入 `release/` 或通过 GitHub Release 管理。
  4. 在 main 分支打标签：
     ```bash
     git checkout main
     git pull origin main
     git tag -a v1.2.0 -m "Release v1.2.0"
     git push origin v1.2.0
     ```

## 3. 变更记录

- 在 `CHANGELOG.md`（可新建）中记录每次版本的新增、修复和变更。
- 对应标签的提交范围应在 changelog 中明确，如：
  ```
  ## [1.2.0] - 2025-01-10
  - 新增：导入导出功能
  - 新增：放弃测验时显示所有答案
  - 优化：数据库管理实时搜索
  ```

## 4. 发布流程（建议）

1. **功能冻结**：完成当次迭代的开发，合并到 develop。
2. **预发布验证**：
   - `./mvnw test`
   - 本地运行 `./mvnw spring-boot:run` 验证核心功能。
   - 检查前端交互。
3. **构建与文档**：
   - `./mvnw clean package`
   - 更新 README、PROJECT_STATUS.md、CHANGELOG.md。
4. **合并与打标签**：将 develop 合并至 main，按第 2 节流程打标签并推送。
5. **发布产物**：上传打包文件或生成 GitHub Release。

## 5. 快速回滚

### 回滚到上一个稳定版本

```bash
git fetch origin
git checkout main
git reset --hard v1.1.0   # 切换到目标版本标签
git push --force origin main   # 如需同步远程，确保团队知情
```
> **谨慎使用** `--force`，如远程已共享可考虑创建 hotfix 分支。

### 撤销单次提交

```bash
# 查看提交历史
git log --oneline
# 回滚指定提交
git revert <commit-hash>
# 合并回 main
git push origin main
```

### 应急热修

1. 从目标发布标签创建 hotfix 分支：`git checkout -b hotfix/v1.2.1 v1.2.0`
2. 修复后测试、打补丁标签 `v1.2.1`
3. 合并回 main，并同步 develop（如果存在）。

## 6. 数据备份与还原（H2 数据库）

- 生产数据位于 `data/typingquiz.mv.db`。
- 发布前后执行备份：
  ```powershell
  Copy-Item -Path data\typingquiz.mv.db -Destination backups\typingquiz_$(Get-Date -Format yyyyMMdd_HHmm).mv.db
  ```
- 回滚数据库：停止应用后，将备份文件覆盖 `data/typingquiz.mv.db` 再启动。

## 7. 自动化脚本（建议）

- `scripts/release.ps1`（或 `.sh`）：封装构建、测试、打标签流程。
- `scripts/backup.ps1`：一键备份 H2 数据库及打包产物。
- `scripts/rollback.ps1`：根据输入的版本号自动 checkout 并还原数据。

## 8. 日常迭代建议

1. 功能开发前创建 issue，描述需求与验收标准。
2. 使用 Pull Request 进行代码评审，确保每次合并可追踪。
3. 每次合并 main 时更新版本标签与 Changelog。
4. 发布前固定执行备份，保证可恢复。

## 9. 标准优化迭代流程（可回滚保障）

1. **计划与分支**：为每个优化项创建 Issue，基于 `main` 或 `develop` 拉取 `feature/…` 分支。
2. **开发与自测**：小步提交，完成后运行 `./mvnw test`、`./mvnw spring-boot:run` 验证核心流程。**本地测试通过后再推送代码**，不在云服务器上调试本地问题。
3. **更新文档与版本**：如需发布，更新 `pom.xml` 版本号及 README、PROJECT_STATUS.md、CHANGELOG.md。
4. **合并与打标签**：通过 PR 合并回 `main`（或先合并 `develop`），随后在 `main` 打上语义化版本标签并推送。
5. **构建与备份**：执行 `./mvnw clean package` 生成产物，按需备份 `data/typingquiz.mv.db`。
6. **发布与记录**：上传 jar 或创建 GitHub Release，附带变更说明与对应用的影响。
7. **回滚策略**：若出现回退需求，使用版本标签 `git reset --hard <tag>` 或 `git revert`，并用备份数据库恢复数据，一定要在团队范围内同步。


---

有了以上流程，即可在任何时间点快速定位版本、回滚到稳定状态，并清晰掌握项目迭代历史。祝开发顺利！
