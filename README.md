# Typing Quiz - JetPunk风格打字测验应用

一个JetPunk风格的打字问答网页应用,玩家通过在单一输入框中输入答案来测试知识。系统在输入正确答案时提供即时反馈,无需特定顺序或手动提交。

## 功能特性

- ✅ 单一输入框答题 - 快速连续输入,无需按回车
- ✅ 即时答案识别 - 输入匹配立即显示
- ✅ 重复答案处理 - 智能识别已答项
- ✅ 无序答题 - 任意顺序回答
- ✅ 时间限制管理 - 可选倒计时功能
- ✅ 详细结果统计 - 准确率、未答项等
- ✅ 大小写不敏感 - 忽略大小写差异
- ✅ **数据持久化** - 数据保存到文件,重启不丢失
- ✅ **测验管理** - 创建、编辑、删除测验
- ✅ **数据库管理** - 按测验或答案维度检索数据
- ✅ **导入导出** - JSON格式导入导出测验数据
- ✅ **放弃功能** - 测验中可放弃并查看所有答案
- ✅ **实时搜索** - 数据库管理支持实时搜索

## 技术栈

### 后端
- Spring Boot 2.7.x
- Spring Data JPA
- H2 Database (嵌入式)
- Maven

### 前端
- HTML5
- CSS3
- Vanilla JavaScript (ES6+)
- Fetch API

## 环境要求

- Java JDK 11 或更高版本 ✅
- Maven 3.6+ (可选,项目包含Maven Wrapper)

## 快速开始

### 启动应用

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

### 访问应用

应用启动后会自动打开浏览器,或手动访问:

- **首页:** http://localhost:8080/home.html (主页导航)
- **测验列表:** http://localhost:8080/quizzes.html
- **测验管理:** http://localhost:8080/manage.html (创建/编辑/删除测验)
- **数据库管理:** http://localhost:8080/database.html (查询和检索数据)
- **直接测验:** http://localhost:8080/index.html?id=1
- **H2控制台:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/typingquiz`
  - 用户名: `sa`
  - 密码: (留空)

## API文档

### 测验相关

#### 获取所有测验
```
GET /api/quizzes
```

#### 获取测验详情
```
GET /api/quizzes/{id}
```

#### 创建测验
```
POST /api/quizzes
Content-Type: application/json

{
  "title": "世界首都",
  "description": "说出世界各国的首都",
  "timeLimit": 600,
  "answers": ["北京", "东京", "伦敦", ...]
}
```

#### 更新测验
```
PUT /api/quizzes/{id}
Content-Type: application/json

{
  "title": "世界首都(更新)",
  "description": "说出世界各国的首都",
  "timeLimit": 300,
  "answers": ["北京", "东京", "伦敦", ...]
}
```

#### 删除测验
```
DELETE /api/quizzes/{id}
```

#### 获取测验答案
```
GET /api/quizzes/{id}/answers
```

### 答案相关

#### 验证答案
```
POST /api/answers/validate
Content-Type: application/json

{
  "quizId": 1,
  "input": "beijing"
}
```

#### 搜索答案
```
GET /api/answers/search?content=北京
```

### 数据库管理相关

#### 获取数据库统计信息
```
GET /api/database/stats
```

#### 按测验ID查询答案
```
GET /api/database/quiz/{id}/answers
```

#### 按测验名称搜索
```
GET /api/database/quiz/search?name=关键字
```

#### 按答案ID查询所属测验
```
GET /api/database/answer/{id}/quiz
```

#### 按答案内容搜索
```
GET /api/database/answer/search?content=关键字
```

### 导入导出相关

#### 导出单个测验
```
GET /api/import-export/quiz/{id}/export
```

#### 导出所有测验
```
GET /api/import-export/quizzes/export
```

#### 导入单个测验
```
POST /api/import-export/quiz/import
Content-Type: application/json

{
  "title": "测验标题",
  "description": "测验描述",
  "timeLimit": 300,
  "answers": ["答案1", "答案2", ...]
}
```

#### 批量导入测验
```
POST /api/import-export/quizzes/import
Content-Type: application/json

[
  {
    "title": "测验1",
    "answers": ["答案1", "答案2"]
  },
  {
    "title": "测验2",
    "answers": ["答案3", "答案4"]
  }
]
```

## 项目结构

```
typing-quiz/
├── src/
│   ├── main/
│   │   ├── java/com/typingquiz/
│   │   │   ├── TypingQuizApplication.java
│   │   │   ├── entity/          # 实体类
│   │   │   ├── repository/      # 数据访问层
│   │   │   ├── service/         # 业务逻辑层
│   │   │   ├── controller/      # 控制器层
│   │   │   └── dto/             # 数据传输对象
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/          # 前端资源
│   └── test/                    # 测试代码
├── pom.xml
├── setup.sh / setup.bat         # 环境检测脚本
├── start.sh / start.bat         # 启动脚本
└── README.md
```

## 开发指南

### 构建项目
```bash
./mvnw clean install
```

### 运行测试
```bash
./mvnw test
```

### 打包应用
```bash
./mvnw package
```

生成的JAR文件位于 `target/typing-quiz-1.0.0.jar`

### 运行打包后的应用
```bash
java -jar target/typing-quiz-1.0.0.jar
```

## 故障排除

### Java未安装
如果看到"未检测到Java JDK"错误:
1. 访问 https://adoptium.net/
2. 下载并安装Java 11或更高版本
3. 重新运行setup脚本

### 端口被占用
如果8080端口被占用,修改 `application.properties`:
```properties
server.port=8081
```

### H2控制台无法访问
确保 `application.properties` 中启用了H2控制台:
```properties
spring.h2.console.enabled=true
```

### 数据库文件位置
数据保存在 `data/` 目录下:
- `data/typingquiz.mv.db` - 数据库文件
- 如需重置数据,删除此文件夹即可

### 备份数据
复制 `data/` 文件夹即可备份所有测验数据

## 许可证

MIT License

## 作者

Typing Quiz Team
