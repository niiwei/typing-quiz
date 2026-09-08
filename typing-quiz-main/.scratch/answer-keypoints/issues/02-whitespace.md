# 02 — 让空格容错在实际作答中生效

Status: complete
Execution: complete

**What to build:** 旧题和新题使用一致的空白、大小写、标点设置，输入法添加空格不再误判。

**Blocked by:** 01

## Acceptance criteria

- [x] 打开忽略空格时 Token数/Token 数、全角空白、NBSP、tab 等价；关闭时差异保留；展示不变。
- [x] 前端本地与后端回退应用相同设置，后端不再用固定 lowerCase 绕过关闭的大小写设置。
- [x] 持久化 normalizedContent 不成为另一套判定口径；不破坏旧题读取。
- [x] 空串及规范化为空的输入不命中；独立标点开关及填空题已有行为有回归证据。

## Execution boundary

执行本票前读取上级规格及工程规则；仅处理本票相关模块和验证。首次执行先确认用户已放行、依赖票已验收、工作区干净。以本票行为完成为提交边界，不开始下一票、不推送、不部署。

## Evidence

提交时补充修改模块、复现失败及修复后通过的命令/结果、兼容验证和剩余风险。截图不能替代 API/数据验证，纯 helper 测试不能替代实际页面消费验证。

## Comments

已完成 02。

- 前端 `normalizeText` 在忽略空格时移除 Unicode White_Space 与 BOM；关闭时只 trim，展示仍读取原始 content。
- 后端验证请求携带三个匹配设置，逐条按当前设置比对，不再依赖单一持久化 `normalizedContent` 或固定小写。
- H2 集成回归覆盖全角空格、开启/关闭空格、空输入和仅符号输入；`mvnw -q -Dtest=QuizBaselineIntegrationTest test` 通过（3 tests, 0 failures）。
- Playwright 浏览器回归覆盖前端真实 `QuizController.normalizeText`；页面 smoke + normalization 共 2 passed。
- 未改填空题匹配实现；现有打字题注释读取仍通过首票基线。

验证命令：

```bash
JAVA_HOME=/opt/homebrew/var/homebrew/tmp/.cellar/openjdk@11/11.0.32.1 sh mvnw -q -Dtest=QuizBaselineIntegrationTest test
python3 -m http.server 8876 --directory src/main/resources/static
MINDPOP_BROWSER_BASE_URL=http://127.0.0.1:8876 npm run test:browser
```
