# 开发贡献指南

感谢您对 敲脑壳 MindPop 项目的关注！本文档帮助开发者了解如何参与项目开发。

## 开发环境搭建

### 前置要求

- Java JDK 11+
- Maven 3.6+（或使用项目自带的 Maven Wrapper）
- MySQL 8.0

### 环境配置

```bash
# 设置环境变量连接数据库
set MYSQL_HOST=47.102.147.127  # Windows
export MYSQL_HOST=localhost      # Linux/Mac

# 启动开发服务器
./mvnw spring-boot:run
```

## Git 工作流

### 分支策略

- `main` - 稳定发布版本
- `feature/xxx` - 功能分支
- `fix/xxx` - 修复分支

### 提交规范

使用 Angular 风格的提交信息：

```bash
# 格式: <type>: <description>
git commit -m "feat: 添加新功能"
git commit -m "fix: 修复某问题"
git commit -m "docs: 更新文档"
git commit -m "refactor: 重构代码"
git commit -m "perf: 性能优化"
```

**类型说明：**
- `feat` - 新功能
- `fix` - 修复
- `docs` - 文档更新
- `refactor` - 重构
- `perf` - 性能优化
- `chore` - 杂项

### 代码提交流程

1. **本地修改代码**
2. **本地测试验证** (`./mvnw clean package`)
3. **测试通过后提交并推送**
4. **部署到云服务器**（如需）

## 添加新功能流程

1. 创建功能分支：`git checkout -b feature/xxx`
2. 在对应层添加代码：
   - `entity/` - 数据模型
   - `repository/` - 数据访问
   - `service/` - 业务逻辑
   - `controller/` - API 端点
   - `dto/` - 请求/响应对象
3. 本地测试
4. 提交并推送到 main

## 修复 Bug 流程

1. 创建修复分支：`git checkout -b fix/xxx`
2. 定位问题代码
3. 本地复现并修复
4. 测试通过后提交

## 代码规范

- 缩进: 4个空格
- 命名: 类名大驼峰，方法/变量小驼峰
- 注释: 中文注释，方法级 JavaDoc
- 所有查询必须过滤 userId，确保数据隔离

## 测试要求

- 功能测试通过后再提交
- 运行 `./mvnw clean package` 构建验证
- 本地环境能复现的问题不在云服务器上调试

## 更新文档

- 修改代码时同步更新 `CHANGELOG.md`
- 如需发布，更新 `pom.xml` 版本号
- 更新相关技术文档

## 注意事项

1. **账户隔离** - 所有数据操作必须验证用户身份
2. **测试优先** - 本地测试通过后再推送
3. **文档同步** - 代码变更同步更新相关文档
4. **版本标签** - 每次发布打标签
