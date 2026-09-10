# 03 — 提供 Codex 可调用的 stdio MCP Server

**What to build:** Codex 可以启动本地标准 stdio MCP Server，并通过结构化工具安全调用敲脑壳 Agent API 完成题库与分组管理。

**Blocked by:** 02 — 交付安全可重试的 Agent API

**Status:** completed

- [x] 独立 TypeScript 模块要求 Node.js 20+，锁定依赖并构建为可由 node 启动的 ESM JavaScript。
- [x] 启动时读取 MINDPOP_BASE_URL 和 MINDPOP_PAT；缺失配置给出可操作错误，日志和凭证不进入 stdout。
- [x] 注册题库列表、详情、创建、局部更新、批量导入、删除预览和删除工具。
- [x] 注册分组列表、详情、创建、局部更新、删除预览、删除及题库移入移出工具。
- [x] 所有工具使用严格输入 schema，返回结构化数据和简短摘要，并标记只读与破坏性属性。
- [x] import_quizzes 要求 UUID request_id，并将值原样映射给 Agent API。
- [x] API 错误映射为稳定 MCP 错误，输出中不包含 PAT。
- [x] 模拟 HTTP 测试验证 Bearer 请求头、错误映射和 stdout 纯净性；MCP Inspector 已完成 stdio 握手、tools/list（15 个工具）及读写工具调用验证。
- [x] 提供构建、Inspector 和 Codex 配置说明，示例配置启用 writes 审批且不含真实 PAT。
