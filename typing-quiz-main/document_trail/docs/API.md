# API 接口文档

本文档描述 敲脑壳 MindPop 项目的所有 REST API 接口。

## 认证方式

所有 API（除登录注册外）需要携带 JWT Token：

```http
Authorization: Bearer <token>
```

---

## 认证相关

### 用户注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

**响应：**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 用户登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**响应：**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## 测验相关

### 获取所有测验

```http
GET /api/quizzes
Authorization: Bearer <token>
```

**响应：**
```json
[
  {
    "id": 1,
    "title": "世界首都",
    "description": "说出世界各国的首都",
    "timeLimit": 600,
    "totalAnswers": 10,
    "createdAt": "2025-12-01T10:00:00"
  }
]
```

### 获取测验详情

```http
GET /api/quizzes/{id}
Authorization: Bearer <token>
```

**响应：**
```json
{
  "id": 1,
  "title": "世界首都",
  "description": "说出世界各国的首都",
  "timeLimit": 600,
  "answers": ["北京", "东京", "伦敦"],
  "createdAt": "2025-12-01T10:00:00"
}
```

### 创建测验

```http
POST /api/quizzes
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "世界首都",
  "description": "说出世界各国的首都",
  "timeLimit": 600,
  "answers": ["北京", "东京", "伦敦", "巴黎"]
}
```

### 更新测验

```http
PUT /api/quizzes/{id}
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "世界首都(更新)",
  "description": "更新后的描述",
  "timeLimit": 300,
  "answers": ["北京", "东京"]
}
```

### 删除测验

```http
DELETE /api/quizzes/{id}
Authorization: Bearer <token>
```

---

## 答案相关

### 验证答案

```http
POST /api/answers/validate
Content-Type: application/json
Authorization: Bearer <token>

{
  "quizId": 1,
  "input": "beijing"
}
```

**响应：**
```json
{
  "valid": true,
  "answer": "北京",
  "message": "回答正确"
}
```

---

## 分组相关

### 获取所有分组

```http
GET /api/groups
Authorization: Bearer <token>
```

### 创建分组

```http
POST /api/groups
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "地理知识",
  "description": "地理相关测验"
}
```

### 更新分组

```http
PUT /api/groups/{id}
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "更新后的名称",
  "description": "更新后的描述"
}
```

### 删除分组

```http
DELETE /api/groups/{id}
Authorization: Bearer <token>
```

---

## 导入导出相关

### 导出所有测验

```http
GET /api/import-export/quizzes/export
Authorization: Bearer <token>
```

**响应：**
```json
[
  {
    "title": "世界首都",
    "description": "说出世界各国的首都",
    "timeLimit": 600,
    "answers": ["北京", "东京"]
  }
]
```

### 批量导入测验

```http
POST /api/import-export/quizzes/import
Content-Type: application/json
Authorization: Bearer <token>

[
  {
    "title": "测验1",
    "description": "描述1",
    "timeLimit": 300,
    "answers": ["答案1", "答案2"]
  },
  {
    "title": "测验2",
    "answers": ["答案3", "答案4"]
  }
]
```

---

## 复习相关

### 获取今日复习总览

```http
GET /api/review/today
Authorization: Bearer <token>
```

**响应：**
```json
{
  "total": 10,
  "new": 3,
  "learning": 2,
  "review": 4,
  "relearning": 1,
  "dueToday": 8
}
```

### 获取分组复习统计

```http
GET /api/review/groups/summary
Authorization: Bearer <token>
```

### 获取分组下测验列表

```http
GET /api/review/groups/{groupId}/quizzes
Authorization: Bearer <token>
```

### 获取全局待复习测验

```http
GET /api/review/quizzes
Authorization: Bearer <token>
```

### 获取下一个待复习测验

```http
GET /api/review/next?groupId={groupId}&currentQuizId={currentQuizId}
Authorization: Bearer <token>
```

### 提交学习评级

```http
POST /api/review/{quizId}/learn
Content-Type: application/json
Authorization: Bearer <token>

{
  "rating": 3
}
```

**评级说明：**
- 1 = 重来 (Again)
- 2 = 困难 (Hard)
- 3 = 良好 (Good)
- 4 = 简单 (Easy)

### 提交复习评级

```http
POST /api/review/{quizId}/review
Content-Type: application/json
Authorization: Bearer <token>

{
  "rating": 3
}
```

### 获取复习统计

```http
GET /api/review/stats
Authorization: Bearer <token>
```

### 获取测验状态

```http
GET /api/review/{quizId}/status
Authorization: Bearer <token>
```

### 重置测验状态

```http
POST /api/review/{quizId}/reset
Authorization: Bearer <token>
```

---

## 错误响应

### 401 Unauthorized

```json
{
  "error": "未授权，请登录"
}
```

### 403 Forbidden

```json
{
  "error": "无权访问此资源"
}
```

### 404 Not Found

```json
{
  "error": "资源不存在"
}
```

### 500 Internal Server Error

```json
{
  "error": "服务器内部错误"
}
```
