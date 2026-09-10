# 敲脑壳本地 MCP

本地 TypeScript stdio MCP Server，要求 Node.js 20+。服务端只通过 `MINDPOP_BASE_URL` 和 `MINDPOP_PAT` 访问敲脑壳 `/api/agent/v1`，PAT 不写入仓库。

```bash
npm install
npm run build
MINDPOP_BASE_URL=https://your-mindpop.example MINDPOP_PAT=mp_pat_... node dist/index.js
```

在 Codex 的共享配置中登记（先在敲脑壳设置页创建 PAT，并只复制创建响应中的一次性明文）：

```toml
[mcp_servers.mindpop]
command = "node"
args = ["/仓库绝对路径/mcp-server/dist/index.js"]
default_tools_approval_mode = "writes"

[mcp_servers.mindpop.env]
MINDPOP_BASE_URL = "https://your-mindpop.example"
MINDPOP_PAT = "mp_pat_..."
```

也可以使用 Codex CLI：

```bash
codex mcp add mindpop \
  --env MINDPOP_BASE_URL=https://your-mindpop.example \
  --env MINDPOP_PAT=mp_pat_... \
  -- node /仓库绝对路径/mcp-server/dist/index.js
```

`dist/` 和 `node_modules/` 是本地构建产物，不提交。stdout 仅用于 MCP 协议，诊断信息写 stderr。
