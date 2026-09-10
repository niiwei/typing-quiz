# 部署指南

当前部署流程以根目录 [`DEPLOY.md`](../DEPLOY.md) 为唯一操作手册，避免 Docker、systemd 和旧服务器路径并存造成误操作。

核心约束：

- `main` 是 GitHub 与生产环境的唯一发布源。
- 合并前通过 GitHub CI；发布前确认本地 `HEAD` 等于 `origin/main`。
- 使用 `./scripts/deploy-main.sh` 从干净 Git 归档构建 Java 与 MCP，上传并重启 `mindpop.service`。
- 生产应用位于 `/opt/mindpop`，配置位于 `/etc/mindpop/mindpop.env`。
- 每次发布保留旧 JAR，并在 `/opt/mindpop/DEPLOYED_COMMIT` 记录提交 SHA。
- 回滚共享代码使用 `git revert`，禁止强推 `main`。

数据库迁移、首次配置、健康检查与回滚命令见根目录部署手册。
