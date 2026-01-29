# 快速开始指南

## 🚀 5分钟快速启动

### 1. 检查Java环境

打开命令行,输入:
```bash
java -version
```

如果显示Java版本信息(需要11或更高),说明Java已安装。

如果未安装,请访问: https://adoptium.net/

### 2. 运行环境检测脚本

**Windows用户:**
```bash
cd typing-quiz
setup.bat
```

**Linux/Mac用户:**
```bash
cd typing-quiz
chmod +x setup.sh
./setup.sh
```

脚本会自动:
- 检测Java和Maven
- 下载项目依赖
- 构建项目

### 3. 启动应用

**Windows用户:**
```bash
start.bat
```

**Linux/Mac用户:**
```bash
chmod +x start.sh
./start.sh
```

### 4. 开始使用

应用启动后会自动在浏览器中打开首页,或手动访问:
- **首页**: http://localhost:8080/home.html
- **测验列表**: http://localhost:8080/quizzes.html
- **测验管理**: http://localhost:8080/manage.html
- **数据库管理**: http://localhost:8080/database.html
- **直接开始**: http://localhost:8080/index.html?id=1

## 🎮 如何玩

1. 选择一个测验(或直接访问默认的"世界首都"测验)
2. 在输入框中输入答案
3. 答案正确会立即显示并计分
4. 尝试在时间限制内答出所有答案!

## 💡 提示

- 答案不区分大小写
- 不需要按回车键,输入匹配即可
- 可以任意顺序回答
- 重复输入会提示"已回答"

## 🔧 常见问题

### 端口被占用
修改 `src/main/resources/application.properties`:
```properties
server.port=8081
```

### 查看数据库
访问 H2 控制台: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/typingquiz`
- 用户名: `sa`
- 密码: (留空)

### 重新初始化数据
删除 `data/` 文件夹,重启应用会重新初始化示例数据

## 📝 创建自己的测验

### 方法1: 使用管理页面(推荐)
访问 http://localhost:8080/manage.html 使用可视化界面创建测验

### 方法2: 使用API
```bash
curl -X POST http://localhost:8080/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "title": "我的测验",
    "description": "测验描述",
    "timeLimit": 300,
    "answers": ["答案1", "答案2", "答案3"]
  }'
```

## 🛑 停止应用

在运行应用的命令行窗口按 `Ctrl+C`

## 📚 更多信息

查看完整文档: [README.md](README.md)
