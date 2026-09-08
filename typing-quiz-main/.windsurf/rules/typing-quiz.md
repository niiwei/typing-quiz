---
trigger: always_on
---

# Windsurf 全局规则 - Typing Quiz 项目

## 1. 框架版本识别
确保代码与项目技术栈版本匹配：
- **后端框架**: Spring Boot 2.7.18
- **Java版本**: Java 11
- **持久层**: Spring Data JPA (跟随Spring Boot版本)
- **数据库**: MySQL 8.0.33
- **安全框架**: Spring Security + JWT
- **前端技术**: 原生 HTML5 + JavaScript (ES6+)，无前端框架
- **构建工具**: Maven 3.x

## 2. 代码风格统一性
保持与现有Java代码风格一致：
- 缩进: 4个空格
- 命名: 类名大驼峰，方法/变量小驼峰，常量全大写下划线
- 注释: 中文注释，方法级JavaDoc说明
- 包结构: controller/service/repository/entity/dto分层清晰

## 3. 修改范围控制
严格遵循指定修改范围：
- 禁止过度重构未提及的代码
- 禁止修改与需求无关的文件
- 保持向后兼容，不破坏现有API
- 修改后自行检查是否引入新Bug

## 4. 上下文依赖
基于已有上下文回答：
- 查阅相关文件后再做判断
- 理解完整业务逻辑再修改
- 关注数据流向和依赖关系

## 5. 完整代码阅读
确保理解完整上下文：
- 阅读相关Controller/Service/Repository
- 理解Entity关系和DTO转换
- 检查前端JS调用逻辑

## 6. 回复规范
使用中文，保持语言一致性
- 所有回复使用中文
- 术语保持一致（如"测验"而非"quiz"）

## 7. 操作说明规范
**重要**: 执行多步骤任务时，每一步操作和执行都必须用中文说明当前在干什么
- 执行命令前解释命令的用途
- 代码修改前说明修改目的
- 配置变更前解释变更原因
- 让用户清楚了解每一步的操作意图

## 8. 解答质量
提供详细的逻辑解释：
- 说明修改原因
- 解释数据流向变化
- 指出潜在风险

## 9. 性能考虑
关注代码性能影响：
- 避免N+1查询问题
- 合理使用JOIN查询
- 减少不必要的数据库往返
- 注意事务边界

## 10. 最佳实践建议
提供相关编程最佳实践：
- 后端分层架构（Controller/Service/Repository）
- DTO数据隔离，不直接暴露Entity
- RESTful API设计规范
- 统一返回格式

## 11. 依赖关系
说明模块间依赖关系：
- Service层不直接暴露给前端
- Repository只被Service调用
- Controller负责参数校验和响应封装

## 12. 测试建议
提供测试相关建议：
- 修改后考虑边界情况
- 关注并发场景（如多用户同时操作）
- 验证数据隔离（userId过滤）

## 13. 文档引用
适时引用官方文档：
- Spring Boot官方文档
- Spring Data JPA查询语法
- Java 11特性

## 14. 代码规范
遵循项目已有规范：
- 静态资源放在resources/static/
- 模板文件放在resources/templates/
- 配置文件使用application.properties（非.yml）

## 15. 向后兼容性
考虑代码兼容性：
- API变更保持向后兼容
- 数据库迁移脚本
- 不破坏现有功能

## 16. 安全与质量保证
关注安全性和错误处理：
- **重要**: 所有查询必须过滤userId，确保数据隔离
- 验证用户权限（用户只能操作自己的数据）
- 防止SQL注入（使用JPA参数绑定）
- 敏感操作添加事务
- 统一异常处理

## 17. 代码质量规范
注重代码注释和复用：
- 复杂逻辑添加注释
- 抽取公共方法避免重复
- 使用有意义的变量名

## 18. 工程化考虑
关注构建和部署策略：
- Maven构建配置
- 静态资源缓存策略
- 日志配置

## 19. 知识点讲解
对重要概念进行详细解释：
- DTO/Entity/VO区别
- 事务传播机制
- JOIN查询优化
- 缓存使用（如适用）

## 20. 项目特殊规范
- **数据隔离**: 所有涉及用户数据的查询必须包含userId过滤
- **复习系统**: 理解QuizReviewStatus状态流转（NEW→LEARNING→REVIEW）
- **时区处理**: 统一使用Asia/Shanghai时区
- **Token验证**: 使用typingquiz_token作为localStorage key

## 21. 项目参考文档

快速了解项目可查阅以下文档：

### 对外文档（document_trail/）
- **README.md** - 项目简介与快速开始
- **CONTRIBUTING.md** - 开发贡献指南（Git工作流、提交规范）
- **CODE_STYLE.md** - 代码规范指南
- **DEPLOYMENT.md** - 部署操作指南（云端Docker部署）
- **SECURITY.md** - 安全政策说明
- **docs/API.md** - REST API接口文档
- **docs/DB_SCHEMA.md** - 数据库表结构设计

### 内部详细文档（docs/internal/）
- **AI_DEVELOPMENT_GUIDE.md** - 项目开发全貌（技术栈、项目结构、API端点、开发规范、部署流程）
- **PROJECT_MANAGEMENT.md** - 项目管理与回滚指引（分支策略、版本管理、发布流程、回滚方案）
- **REVIEW_SYSTEM_DESIGN.md** - 复习系统设计与算法说明
- **REVIEW_FEATURE_DESIGN.md** - 复习功能实施计划（功能架构、状态流转、API设计）
- **ARCHITECTURE_DETAIL.md** - 系统架构详细设计
- **CHANGELOG_HISTORY.md** - 完整版本历史记录

## 21. 本地启动方法

### 快捷启动（推荐）
- **Windows**: 双击 `start.bat`
- **PowerShell**: 运行 `.\start.ps1`（自动释放端口并设置环境变量）

### Maven 命令行启动
- **CMD**:
  ```bash
  set MYSQL_HOST=47.102.147.127 && .\mvnw.cmd spring-boot:run
  ```
- **PowerShell**:
  ```powershell
  $env:MYSQL_HOST="47.102.147.127"; .\mvnw.cmd spring-boot:run
  ```

### IDE 启动 (IntelliJ IDEA)
在 **Run/Debug Configurations** -> **Environment variables** 中添加：
`MYSQL_HOST=47.102.147.127`
然后运行 `TypingQuizApplication.java`

## 22. 云端部署方法

项目使用 **SSH + Docker** 部署到云服务器 `47.102.147.127`。

### 自动部署（推荐）

**前置条件**：已配置 SSH 免密登录（密钥位置：`C:\Users\29982\.ssh\typing_quiz_deploy`）

**执行步骤**：
1. **提交代码到 Git**：先将本地修改提交并推送到 main 分支
2. **执行自动部署命令**：在 PowerShell 中运行以下命令，通过 SSH 连接到服务器并执行部署脚本
   ```powershell
   ssh -i C:\Users\29982\.ssh\typing_quiz_deploy root@47.102.147.127 "cd /app/typing-quiz && git pull && docker build -t typing-quiz-app . && docker rm -f typing-quiz-app && docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app"
   ```
3. **等待部署完成**：命令会依次执行拉取代码、构建 Docker 镜像、删除旧容器、启动新容器
4. **验证部署结果**：访问 http://47.102.147.127:8080 确认应用正常运行

**说明**：
- 仅修改前端文件（HTML/JS/CSS）时，无需执行 `docker build`，刷新浏览器即可生效
- 修改后端 Java 代码时，必须执行完整部署流程重新打包

### 手动部署

**适用场景**：自动部署失败或需要精细控制部署过程时

**执行步骤**：
1. **SSH 登录服务器**：使用 SSH 连接到云服务器
   ```bash
   ssh root@47.102.147.127
   ```
2. **进入项目目录**：切换到应用部署目录
   ```bash
   cd /app/typing-quiz
   ```
3. **拉取最新代码**：从 Git 仓库获取最新代码
   ```bash
   git pull
   ```
4. **构建 Docker 镜像**：基于最新代码构建应用镜像（后端 Java 代码变更时必须执行）
   ```bash
   docker build -t typing-quiz-app .
   ```
5. **删除旧容器**：停止并删除正在运行的旧容器
   ```bash
   docker rm -f typing-quiz-app
   ```
6. **启动新容器**：使用新镜像启动应用容器
   ```bash
   docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app
   ```
7. **验证部署结果**：检查容器运行状态并访问应用
   ```bash
   docker ps
   ```

**部署配置说明**：
- **服务器地址**：`47.102.147.127`
- **SSH 端口**：默认 22
- **应用端口**：8080
- **部署目录**：`/app/typing-quiz`
- **数据库**：MySQL 8.0（位于同一服务器）

**注意事项**：
- 部署前确保代码已在本地测试通过
- 首次部署需在服务器上配置 SSH 公钥到 `~/.ssh/authorized_keys`
- 部署过程中如遇端口冲突，先停止占用 8080 端口的进程

## 23. 更新日志维护规范

**重要**: 每次代码变动/修改完成后，必须同步更新以下两个CHANGELOG文件：

### 对外文档（简洁版）
- **文件路径**: `document_trail/CHANGELOG.md`
- **更新时机**: 功能发布、版本迭代时
- **内容要求**: 简洁明了，突出功能点和用户价值
- **格式示例**:
  ```markdown
  ## [v1.9.0] - 2026-02-23
  ### 测验分组体系深度重构
  - 取消"未分组"状态，所有测验强制归属分组
  - 新测验自动归入"默认分组"
  ```

### 内部文档（详细版）
- **文件路径**: `docs/internal/CHANGELOG_HISTORY.md`
- **更新时机**: 每次Bug修复、功能实现完成后立即更新
- **内容要求**: 详细记录问题描述、根因分析、解决方案、修改文件
- **格式示例**:
  ```markdown
  #### 问题标题 
  **问题描述**
  具体描述问题现象

  **根因分析**
  详细分析原因

  **解决方案**
  具体的修复措施

  **修改文件**
  - `文件路径` - 修改说明
  ```

### 双轨更新原则
1. **Bug修复**: 必须同时在两个CHANGELOG中记录（对外简述问题，对内详细记录根因）
2. **功能新增**: 对外文档重点描述功能价值，对内文档记录技术实现细节
3. **版本号**: 保持两个文档的版本号和时间一致
4. **Git提交**: 更新CHANGELOG后一并提交到Git