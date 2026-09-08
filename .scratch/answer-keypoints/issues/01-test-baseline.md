# 01 — 建立可重复的隔离验证入口

Status: ready-for-agent
Execution: complete

**What to build:** 在不连接真实数据库的条件下，执行旧题创建、读取和校验基线，为后续功能提供可运行验收。

**Blocked by:** None — can start immediately

## Acceptance criteria

- [x] 准备可用 JDK 11 和 Maven Wrapper；依赖放系统缓存或仓库外，不纳入源码提交。记录版本及准确运行命令。
- [x] 增加隔离测试配置和必要的测试数据库依赖；测试禁止读取真实 application.properties 凭据，禁止 DataInitializer 污染真实数据。
- [x] 建立旧 TYPING/注释导入读取校验往返的后端基线，以及真实打字页面的浏览器测试入口；记录通过数量，零测试不算通过。
- [x] 记录隔离启动、测试和清理方法；失败时定位环境原因，不以跳过测试替代。

## Execution boundary

执行本票前读取上级规格及工程规则；仅处理本票相关模块和验证。首次执行先确认用户已放行、依赖票已验收、工作区干净。以本票行为完成为提交边界，不开始下一票、不推送、不部署。

## Evidence

提交时补充修改模块、复现失败及修复后通过的命令/结果、兼容验证和剩余风险。截图不能替代 API/数据验证，纯 helper 测试不能替代实际页面消费验证。

## Comments

已完成首票基线。

- 使用 Homebrew 用户目录中的 OpenJDK `11.0.32.1`，并通过工程 `sh mvnw` 运行 Maven Wrapper；JDK、Maven 依赖和 Playwright 浏览器均不纳入 Git。
- 增加测试 profile、H2 隔离数据库和 `mindpop.data-initializer.enabled=false` 开关；测试不读取生产凭据，也不扫描 `initial-data`。
- 后端真实路径测试通过：创建旧 TYPING 题、读取题目及 comment、提交 `/api/answers/validate`，`mvnw test` 结果为 1 tests, 0 failures, 0 errors。
- 真实静态页面浏览器 smoke 通过：`MINDPOP_BROWSER_BASE_URL=http://127.0.0.1:8876 npm run test:browser`，结果为 1 passed；页面由 Python 静态服务器提供，未连接线上服务。
- 首次基线测试暴露匿名创建时默认分组 userId 为空的问题；测试改用隔离 JWT 用户上下文，未修改产品行为。

验证命令：

```bash
JAVA_HOME=/opt/homebrew/var/homebrew/tmp/.cellar/openjdk@11/11.0.32.1 sh mvnw -q test
python3 -m http.server 8876 --directory src/main/resources/static
MINDPOP_BROWSER_BASE_URL=http://127.0.0.1:8876 npm run test:browser
```
