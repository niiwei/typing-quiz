# 安全说明

本文档描述 Typing Quiz 项目的安全政策、最佳实践和漏洞报告流程。

## 1. 安全政策

### 1.1 账户安全

- **密码加密**：使用 BCrypt 加密存储，永不明文保存
- **JWT Token**：24小时有效期，使用 HS256 签名
- **Token 存储**：前端使用 localStorage（typingquiz_token）
- **密码强度提示**：前端提供密码强度可视化提示（仅参考，不做强制限制）

### 1.2 数据隔离

- **用户数据隔离**：所有查询必须按 userId 过滤
- **权限验证**：后端 API 强制校验当前登录用户权限
- **禁止跨用户访问**：用户只能访问自己的测验和分组

### 1.3 传输安全

- **HTTPS 建议**：生产环境建议使用 HTTPS
- **CORS 配置**：当前开放跨域，生产环境应限制域名

## 2. 已实施的安全措施

### 2.1 认证安全

```java
// JWT 签名密钥
private static final String SECRET = "typingquizsecretkey2024";
private static final long EXPIRATION = 86400000L; // 24小时

// Token 结构：userId:username
// 示例：1:admin
```

### 2.2 密码安全

```java
// BCrypt 加密
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode(password);

// 验证
encoder.matches(rawPassword, hashedPassword);
```

### 2.3 SQL 注入防护

```java
// 使用参数绑定，禁止字符串拼接
@Query("SELECT q FROM Quiz q WHERE q.title = :title AND q.userId = :userId")
List<Quiz> findByTitleAndUserId(@Param("title") String title, @Param("userId") Long userId);
```

### 2.4 XSS 防护

- 前端用户输入进行转义
- 后端返回数据时进行 HTML 转义

## 3. 漏洞报告

### 3.1 报告方式

如果您发现安全漏洞，请通过以下方式报告：

1. **不要公开披露**：请勿在公开渠道发布漏洞信息
2. **发送邮件**：security@typingquiz.example.com
3. **提供详情**：
   - 漏洞描述
   - 复现步骤
   - 影响范围
   - 可能的修复建议

### 3.2 响应时间

| 严重程度 | 响应时间 | 修复目标 |
|----------|----------|----------|
| 严重 | 24小时内 | 3天内 |
| 高危 | 48小时内 | 7天内 |
| 中危 | 72小时内 | 14天内 |
| 低危 | 7天内 | 30天内 |

### 3.3 安全公告

修复后的安全更新将记录在 CHANGELOG.md 中。

## 4. 安全最佳实践

### 4.1 开发阶段

- **代码审查**：所有提交必须经过审查
- **依赖更新**：定期更新依赖库，修复已知漏洞
- **安全测试**：包含安全相关的测试用例
- **最小权限**：数据库账户只授予必要权限

### 4.2 部署阶段

- **环境分离**：开发、测试、生产环境隔离
- **配置加密**：敏感配置（密码、密钥）不提交到 Git
- **HTTPS 强制**：生产环境强制 HTTPS
- **日志脱敏**：日志中不记录敏感信息（密码、Token）

### 4.3 运维阶段

- **定期备份**：数据库和代码定期备份
- **监控告警**：异常访问模式告警
- **安全审计**：定期审查访问日志
- **漏洞扫描**：定期使用工具扫描漏洞

## 5. 已知限制

### 5.1 当前已知风险

- **CORS 开放**：当前允许所有跨域请求，生产环境应限制
- **Token 存储**：Token 存储在 localStorage，存在 XSS 风险
- **密码复杂度**：前端仅做基础验证，后端需加强验证

### 5.2 改进计划

- [ ] 添加 HTTPS 支持
- [ ] 限制 CORS 域名
- [ ] 添加 Rate Limiting
- [ ] 实现 Token 刷新机制
- [ ] 添加登录失败锁定

## 6. 安全相关配置

### 6.1 推荐配置

```properties
# 生产环境建议
server.ssl.enabled=true
server.ssl.key-store=keystore.p12
server.ssl.key-store-password=your-password

# CORS 限制
cors.allowed-origins=https://your-domain.com

# 会话管理
server.servlet.session.timeout=30m
```

## 7. 参考资源

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security 文档](https://docs.spring.io/spring-security/reference/)
- [JWT 安全最佳实践](https://jwt.io/security)

---

**最后更新**：2026-02-20

**维护者**：Typing Quiz Team
