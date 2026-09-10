# Agent API 与 MCP 接入

MindPop 为本地 Agent 提供 stdio MCP Server。MCP 通过 HTTPS 调用 `/api/agent/v1`，浏览器继续使用 JWT；两类凭证不能跨路径使用。

## 创建凭证

用户登录网页后，在设置页创建命名 PAT。完整 `mp_pat_...` 只在创建响应中出现一次，服务端仅保存哈希；撤销后立即失效。PAT 只能访问 `/api/agent/v1/**`。

## 启动 MCP

```bash
cd mcp-server
npm ci
npm run build
MINDPOP_BASE_URL=https://mindpop.top \
MINDPOP_PAT=mp_pat_... \
node dist/index.js
```

Codex 配置示例见 `mcp-server/README.md`。PAT 只放在本机 Codex 配置或环境变量中。

## 能力范围

- 查询、创建和更新 TYPING 题库；列表会返回其他题型并用 `writable` 表示是否可写。
- 查询、创建和更新分组，以及添加或解除题库关联。
- 使用 UUID `requestId` 原子、幂等导入题库；同一次逻辑重试必须复用 UUID。
- 删除题库或分组前先获取五分钟有效的确认令牌；分组删除只解除关联。

本期不支持 FILL_BLANK 写入、作答、学习、复习和统计。

## HTTP 约定

所有 Agent 请求使用：

```http
Authorization: Bearer mp_pat_...
```

错误统一为：

```json
{
  "code": "VERSION_CONFLICT",
  "message": "题库版本已变化",
  "details": {}
}
```

更新必须携带当前 `version`。版本落后返回 `409`；访问不存在或其他用户资源统一返回 `404`；无效或撤销的 PAT 返回 `401`。

## 导入后的核对

题库整理 Skill 只有在用户明确要求“导入敲脑壳”时才调用 `import_quizzes`。成功后按返回 ID 调用 `get_quiz`，核对题库数、答案数和分组；失败时保留已验证 JSON，不改用网页自动点击。
