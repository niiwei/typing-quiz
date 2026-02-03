# ngrok 内网穿透指南

## 什么是内网穿透？

内网穿透是让外网（互联网）用户访问你本地电脑服务的技术。

```
用户B (外网)
    ↓ 访问 ngrok 域名
ngrok 服务器 (中转)
    ↓ 转发请求
你的电脑 (localhost:8080) ← 数据保存在这里
    ↓
data/typingquiz.mv.db
```

## 使用场景

- 快速分享给朋友测试
- 远程演示项目
- 临时公开访问本地服务

## 安装配置步骤

### 1. 下载 ngrok

1. 访问 https://ngrok.com/download
2. 下载 Windows 版本
3. 解压到 `C:\Program Files\ngrok-v3-stable-windows-amd64\`

### 2. 注册账号获取 Token

1. 访问 https://ngrok.com 注册账号
2. 进入 https://dashboard.ngrok.com/get-started/your-authtoken
3. 复制你的 Authtoken（类似 `39xxxxx`）

### 3. 配置 Authtoken

在 PowerShell 执行：

```bash
"C:\Program Files\ngrok-v3-stable-windows-amd64\ngrok.exe" config add-authtoken YOUR_TOKEN
```

### 4. 启动 Spring Boot 应用

确保你的项目正在运行：

```bash
.\mvnw.cmd spring-boot:run
```

验证端口监听：

```bash
netstat -ano | findstr :8080
```

应看到 `LISTENING` 状态。

### 5. 启动 ngrok 穿透

在另一个 PowerShell 窗口执行：

```bash
"C:\Program Files\ngrok-v3-stable-windows-amd64\ngrok.exe" http 8080
```

### 6. 获取公网地址

启动后会看到类似输出：

```
Session Status                online
Account                       your_name (Plan: Free)
Version                       3.36.0
Region                        Asia Pacific (ap)
Latency                       110ms
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://xxxx.ngrok-free.dev -> http://localhost:8080
```

**Forwarding** 后面的 `https://xxxx.ngrok-free.dev` 就是你的公网访问地址。

### 7. 访问验证

- **公网访问**：浏览器打开 `https://xxxx.ngrok-free.dev`
- **管理面板**：访问 `http://127.0.0.1:4040` 查看连接状态

## 常见问题

### ERR_NGROK_8012 - 连接被拒绝

**原因**：本地 8080 端口没有服务运行

**解决**：

```bash
# 检查端口
netstat -ano | findstr :8080

# 如果没有 LISTENING，启动应用
.\mvnw.cmd spring-boot:run
```

### 域名每次都变

**原因**：免费版每次启动会分配随机域名

**解决**：
- 付费版可绑定固定域名
- 或记录当前域名分享给他人

### 免费版限制

| 限制项 | 说明 |
|--------|------|
| 随机域名 | 每次重启 ngrok 会变 |
| 并发连接 | 有限制 |
| 连接时长 | 8小时自动断开 |
| 版本支持 | 2026/2/17 后只支持 v3.19+ |

## 安全提示

⚠️ **重要**：

1. **Authtoken 保密** - 不要泄露给他人，泄露后请立即在 ngrok 控制台撤销
2. **仅用于测试** - 内网穿透不适合生产环境
3. **数据本地存储** - 用户创建的数据仍保存在你电脑
4. **关闭即断网** - 你关闭电脑或 ngrok 后，外网无法访问

## 停止服务

按 `Ctrl+C` 关闭 ngrok，或关闭 ngrok 窗口即可停止穿透。

要重新开启，只需再次执行：

```bash
"C:\Program Files\ngrok-v3-stable-windows-amd64\ngrok.exe" http 8080
```

---

*最后更新：2026-02-04*
