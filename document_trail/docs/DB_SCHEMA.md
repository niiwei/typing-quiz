# 数据库设计文档

本文档描述 Typing Quiz 项目的数据库表结构设计。

## 1. 数据库概览

- **数据库类型**: MySQL 8.0
- **字符集**: utf8mb4_unicode_ci
- **数据库名**: typing_quiz

## 2. 实体关系图

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (PK)         │
│ username        │
│ email           │
│ password        │
│ created_at      │
│ updated_at      │
└─────────────────┘
       │
       │ 1:N
       ↓
┌─────────────────┐
│      quiz       │
├─────────────────┤
│ id (PK)         │
│ title           │
│ description     │
│ time_limit      │
│ user_id (FK)    │
│ quiz_type       │
│ created_at      │
└─────────────────┘
       │
       │ 1:N
       ↓
┌─────────────────┐
│     answer      │
├─────────────────┤
│ id (PK)         │
│ quiz_id (FK)    │
│ content         │
│ normalized_content│
│ comment         │
└─────────────────┘
```

```
┌─────────────────┐
│     users       │
└─────────────────┘
       │
       │ 1:N
       ↓
┌─────────────────┐
│   quiz_group    │
├─────────────────┤
│ id (PK)         │
│ name            │
│ description     │
│ user_id (FK)    │
│ display_order   │
└─────────────────┘
       │
       │ M:N
       ↓
┌─────────────────────┐
│   quiz_group_quiz   │
├─────────────────────┤
│ group_id (FK)       │
│ quiz_id (FK)        │
└─────────────────────┘
```

## 3. 表结构详情

### 3.1 users (用户表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 邮箱 |
| password | VARCHAR(255) | NOT NULL | 加密密码 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

**索引：**
- `idx_username` - 用户名索引
- `idx_email` - 邮箱索引

---

### 3.2 quiz (测验表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(255) | NOT NULL | 测验标题 |
| description | TEXT | | 测验描述 |
| time_limit | INT | | 时间限制（秒） |
| user_id | BIGINT | FK | 创建者ID |
| quiz_type | VARCHAR(20) | NOT NULL, DEFAULT 'TYPING' | 测验类型 |
| created_at | DATETIME | NOT NULL | 创建时间 |

**索引：**
- `idx_user_id` - 用户ID索引（用于账户隔离查询）
- `idx_title` - 标题索引（用于搜索）

**外键：**
- `fk_quiz_user` → `users(id)`

---

### 3.3 answer (答案表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| quiz_id | BIGINT | FK, NOT NULL | 所属测验ID |
| content | VARCHAR(255) | NOT NULL | 原始内容 |
| normalized_content | VARCHAR(255) | NOT NULL | 标准化内容（小写） |
| comment | VARCHAR(500) | | 答案注释 |

**索引：**
- `idx_quiz_id` - 测验ID索引
- `idx_normalized_content` - 标准化内容索引（用于匹配）

**外键：**
- `fk_answer_quiz` → `quiz(id)` ON DELETE CASCADE

---

### 3.4 quiz_group (测验分组表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 分组名称 |
| description | TEXT | | 分组描述 |
| user_id | BIGINT | FK, NOT NULL | 所有者ID |
| display_order | INT | DEFAULT 0 | 显示顺序 |

**索引：**
- `idx_user_id` - 用户ID索引

**外键：**
- `fk_group_user` → `users(id)`

---

### 3.5 quiz_group_quiz (分组-测验关联表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| group_id | BIGINT | FK, PK | 分组ID |
| quiz_id | BIGINT | FK, PK | 测验ID |

**主键：**
- 复合主键 `(group_id, quiz_id)`

**外键：**
- `fk_qgq_group` → `quiz_group(id)` ON DELETE CASCADE
- `fk_qgq_quiz` → `quiz(id)` ON DELETE CASCADE

---

### 3.6 quiz_review_status (复习状态表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| quiz_id | BIGINT | NOT NULL | 测验ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| status | VARCHAR(20) | NOT NULL | 状态（NEW/LEARNING/REVIEW/RELEARNING/SUSPENDED） |
| interval_days | INT | DEFAULT 0 | 间隔天数 |
| ease_factor | INT | DEFAULT 2500 | 简易度（2.5倍 = 2500） |
| next_review_date | DATETIME | | 下次复习时间 |
| buried_until | DATETIME | | 搁置截止日期（null表示未搁置） |
| last_review_date | DATETIME | | 上次复习时间 |
| review_count | INT | DEFAULT 0 | 复习次数 |
| lapse_count | INT | DEFAULT 0 | 遗忘次数（重来次数） |
| learning_step | INT | DEFAULT 0 | 学习阶段步数（0表示未开始） |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

**索引：**
- `idx_quiz_user` - 复合索引 `(quiz_id, user_id)`，唯一约束
- `idx_user_id` - 用户ID索引
- `idx_status` - 状态索引
- `idx_next_review` - 下次复习时间索引

---

### 3.7 fill_blank_quiz (填空题表)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| quiz_id | BIGINT | FK, UNIQUE, NOT NULL | 关联测验ID |
| full_text | TEXT | NOT NULL | 完整文本 |
| blanks_info | TEXT | NOT NULL | 挖空信息（JSON格式） |
| display_text | TEXT | NOT NULL | 显示文本（___替换） |
| blanks_count | INT | NOT NULL | 挖空数量 |
| created_at | DATETIME | NOT NULL | 创建时间 |

**外键：**
- `fk_fbq_quiz` → `quiz(id)` ON DELETE CASCADE

## 4. 索引设计原则

### 4.1 账户隔离索引

所有涉及用户数据的表都必须有 `user_id` 索引：

```sql
CREATE INDEX idx_quiz_user_id ON quiz(user_id);
CREATE INDEX idx_group_user_id ON quiz_group(user_id);
```

### 4.2 查询优化索引

```sql
-- 答案匹配查询
CREATE INDEX idx_answer_normalized ON answer(normalized_content);

-- 复习状态查询
CREATE INDEX idx_review_status ON quiz_review_status(user_id, status);
CREATE INDEX idx_review_next_date ON quiz_review_status(user_id, next_review_date);
```

## 5. 初始化 SQL

### 5.1 创建数据库

```sql
CREATE DATABASE typing_quiz 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 5.2 创建用户

```sql
CREATE USER 'typingquiz'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON typing_quiz.* TO 'typingquiz'@'%';
FLUSH PRIVILEGES;
```

## 6. 数据备份与恢复

### 6.1 备份

```bash
mysqldump -h localhost -u typingquiz -p typing_quiz > backup.sql
```

### 6.2 恢复

```bash
mysql -h localhost -u typingquiz -p typing_quiz < backup.sql
```
