# 敲脑壳 MindPop - 专为复杂知识设计的硬核记忆工具

一个专为复杂、长清单知识设计的「Anki 升级版」提取练习工具。采用"单框连击"交互，无需手动提交，只有当你真正敲对关键词，答案才会砰然跳出，彻底杜绝自我欺骗。

## 功能特性

- ✅ **强制主动输出** - "单框连击"交互，敲对才显示答案，切断"看一眼"的退路
- ✅ **科学间隔复习** - 基于 Anki SM-2 算法，自动规划复习任务
- ✅ **结构化记忆** - 完美适配历史、法条、医学等"一对多"复杂场景
- ✅ **无序答题** - 任意顺序回答
- ✅ 时间限制管理 - 可选倒计时功能
- ✅ 详细结果统计 - 准确率、未答项等
- ✅ 大小写不敏感 - 忽略大小写差异
- ✅ **数据持久化** - 数据保存到MySQL,重启不丢失
- ✅ **测验管理** - 创建、编辑、删除测验
- ✅ **数据库管理** - 按测验或答案维度检索数据
- ✅ **导入导出** - JSON格式导入导出测验数据
- ✅ **放弃功能** - 测验中可放弃并查看所有答案
- ✅ **实时搜索** - 数据库管理支持实时搜索
- ✅ **测验分组** - 创建分组并自由关联测验
- ✅ **答案注释** - 为答案添加注释，答题时忽略注释部分
- ✅ **间隔重复复习** - 基于Anki SM-2算法的复习系统

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.18
- **数据持久化**: Spring Data JPA
- **数据库**: MySQL 8.0
- **认证**: JWT (JJWT) + Spring Security (BCrypt)
- **构建工具**: Maven

### 前端
- HTML5
- CSS3
- Vanilla JavaScript (ES6+)
- Fetch API

## 环境要求

- Java JDK 11 或更高版本
- Maven 3.6+ (可选,项目包含Maven Wrapper)
- MySQL 8.0 (本地或远程)

## 快速开始

### 1. 检查 Java 环境

```bash
java -version
```

如果未安装,请访问: https://adoptium.net/

### 2. 环境检测

**Windows:**
```bash
setup.bat
```

**Linux/Mac:**
```bash
chmod +x setup.sh
./setup.sh
```

### 3. 启动应用

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

应用将在 http://localhost:8080 启动

### 4. 访问应用

- **首页:** http://localhost:8080/home.html
- **测验列表:** http://localhost:8080/quizzes.html
- **测验管理:** http://localhost:8080/manage.html
- **数据库管理:** http://localhost:8080/database.html
- **注册/登录:** http://localhost:8080/register.html

## 项目结构

```
typing-quiz/
├── src/main/java/com/typingquiz/
│   ├── entity/           # 实体类
│   ├── repository/       # 数据访问层
│   ├── service/         # 业务逻辑层
│   ├── controller/      # REST API 控制器
│   ├── dto/             # 数据传输对象
│   ├── config/          # 配置类
│   ├── exception/       # 异常处理
│   └── util/            # 工具类
├── src/main/resources/
│   ├── application.properties
│   └── static/          # 前端资源
├── pom.xml
├── Dockerfile
└── README.md
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [API 文档](docs/API.md) | 完整 REST API 接口说明 |
| [数据库设计](docs/DB_SCHEMA.md) | 数据库表结构设计 |
| [部署指南](DEPLOYMENT.md) | 部署流程和环境配置 |
| [开发贡献指南](CONTRIBUTING.md) | 开发规范和 Git 工作流 |
| [代码规范](CODE_STYLE.md) | 编码规范和命名约定 |
| [安全说明](SECURITY.md) | 安全政策和漏洞报告 |

**详细技术文档**请参阅 `docs/internal/` 目录：
- 系统架构详细设计
- 完整版本历史记录
- AI 开发指导
- 项目管理规范
- 算法实现细节

## 安全设计

### JWT 认证
- Token 有效期: 24小时
- 签名算法: HS256
- 前端请求需携带: `Authorization: Bearer <token>`

### 账户数据隔离
- 所有数据查询按 `userId` 过滤
- 用户只能访问自己的测验和分组

### 密码加密
- 使用 BCrypt (Spring Security)

## 许可证

MIT License

## 作者

敲脑壳 MindPop Team
