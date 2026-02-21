# 代码规范

本文档定义 Typing Quiz 项目的代码编写规范。

## 1. 基础规范

### 缩进与格式

- **缩进**：4个空格（非Tab）
- **换行**：Unix风格（LF）
- **文件编码**：UTF-8
- **行尾空格**：删除

### 文件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 类文件 | 大驼峰 | `QuizController.java` |
| 包名 | 全小写 | `com.typingquiz.service` |
| 配置文件 | 小写下划线 | `application.properties` |
| 静态资源 | 小写中划线 | `quiz-controller.js` |

## 2. Java 代码规范

### 命名规范

```java
// 类名：大驼峰
public class QuizController { }

// 方法名：小驼峰
public Quiz getQuizById(Long id) { }

// 变量名：小驼峰
Long userId = 1L;

// 常量：全大写下划线
private static final String JWT_SECRET = "secret";
```

### 注释规范

```java
/**
 * 获取测验详情
 * 包含测验基本信息和关联答案
 * 
 * @param id 测验ID
 * @param userId 当前用户ID（用于权限验证）
 * @return 测验详情DTO
 * @throws RuntimeException 无权访问时抛出
 */
public QuizResponseDTO getQuiz(Long id, Long userId) {
    // 验证用户权限
    Quiz quiz = quizRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("测验不存在"));
    
    // 检查数据归属
    if (!userId.equals(quiz.getUserId())) {
        throw new RuntimeException("无权访问此测验");
    }
    
    return convertToDTO(quiz);
}
```

### 包结构规范

```
com.typingquiz/
├── controller/       # REST API 控制器
├── service/        # 业务逻辑层
├── repository/     # 数据访问层
├── entity/         # 实体类
├── dto/            # 数据传输对象
├── config/         # 配置类
├── exception/      # 异常处理
└── util/           # 工具类
```

## 3. 前端代码规范

### JavaScript 命名

```javascript
// 变量：小驼峰
let currentQuiz = null;

// 常量：全大写下划线
const API_BASE = '/api';

// 函数：小驼峰
function validateAnswer(input) { }

// 类：大驼峰
class QuizController { }
```

### HTML/CSS 规范

```html
<!-- 类名：小写中划线 -->
<div class="quiz-container">
    <input class="answer-input" type="text">
</div>
```

```css
/* 选择器：小写中划线 */
.quiz-container {
    max-width: 800px;
    margin: 0 auto;
}
```

## 4. Git 提交规范

### 提交信息格式

```
<type>: <subject>

<body>

<footer>
```

### 类型说明

| 类型 | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 添加间隔重复复习功能` |
| `fix` | 修复问题 | `fix: 修复填空题加载失败` |
| `docs` | 文档更新 | `docs: 更新API文档` |
| `refactor` | 重构 | `refactor: 优化查询性能` |
| `perf` | 性能优化 | `perf: 减少数据库查询次数` |
| `chore` | 杂项 | `chore: 更新依赖版本` |

### 示例

```bash
git commit -m "feat: 添加测验分组功能

- 支持创建分组
- 支持测验关联到分组
- 支持分组排序

Closes #123"
```

## 5. 安全规范

### 数据隔离

```java
// 所有查询必须过滤 userId
public List<Quiz> getUserQuizzes(Long userId) {
    return quizRepository.findByUserId(userId);
}
```

### 密码处理

```java
// 使用 BCrypt 加密
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode(password);
```

### SQL 注入防护

```java
// 使用参数绑定，禁止字符串拼接
@Query("SELECT q FROM Quiz q WHERE q.title = :title")
List<Quiz> findByTitle(@Param("title") String title);
```

## 6. 异常处理规范

```java
// Service 层抛出业务异常
try {
    quizRepository.save(quiz);
} catch (DataIntegrityViolationException e) {
    throw new RuntimeException("测验标题已存在");
}

// Controller 层统一处理
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", e.getMessage()));
}
```

## 7. 日志规范

```java
private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

// 使用占位符，避免字符串拼接
logger.info("创建测验: {}, 用户: {}", title, userId);
logger.error("测验查询失败: quizId={}", id, e);
```

## 8. 测试规范

```java
@Test
public void testCreateQuiz() {
    // Given
    QuizDTO dto = new QuizDTO("测试", null, 60);
    
    // When
    Quiz result = quizService.createQuiz(dto, 1L);
    
    // Then
    assertNotNull(result);
    assertEquals("测试", result.getTitle());
}
```
