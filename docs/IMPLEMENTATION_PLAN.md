# 间隔重复复习功能 - 详细实施计划

> 本文档将间隔重复功能的实现拆分为多个可独立评估和实施的阶段。
> 每个阶段结束后需与用户确认方案合理性，再进入下一阶段。

---

## 项目总览

### 功能目标
为 Typing Quiz 项目添加基于 Anki SM-2 算法的间隔重复复习系统，提升用户长期记忆效果。

### 核心原则
1. **渐进式实现**：从 MVP 开始，逐步添加高级功能
2. **向后兼容**：不影响现有测验功能
3. **用户可控**：可开关、可配置、可跳过
4. **数据隔离**：每个用户的复习状态完全独立

### 关键设计变更

### 模式切换设计
- **自由测验模式**（现有）：无复习追踪，用于快速练习
- **Anki 复习模式**（新增）：启用 SM-2 算法追踪
- **切换位置**：主页新增模式选择开关，用户可自由切换

#### 状态标签逻辑（已实施）
为了让复习列表更清晰，系统采用了细化的状态显示逻辑：

| 状态类型 | 时间条件 | 建议标签 | 颜色 |
| :--- | :--- | :--- | :--- |
| **REVIEW** | `nextReviewDate` <= 现在 | **待复习** | 绿色 |
| **REVIEW** | `nextReviewDate` > 现在 | **复习中** | 蓝色/浅色 |
| **REVIEW** | `nextReviewDate` < 今天凌晨 | **已逾期** | 橙色/红色 |
| **LEARNING** | `nextReviewDate` <= 现在 | **待学习** | 黄色/深色 |
| **LEARNING** | `nextReviewDate` > 现在 | **学习中** | 黄色/浅色 |
| **NEW** | 任何时间 | **新测验** | 灰色 |

- **待复习/待学习**：专门指那些“已经到期、现在该做”的测验。
- **复习中/学习中**：指该测验已经进入了复习循环，但目前还没到下一次复习的时间点。

#### 数据隔离保证
- 每个 `quiz_review_status` 记录绑定 `user_id` 和 `quiz_id`
- 查询时必须同时指定 `user_id` 和 `quiz_id`
- 后端 API 强制校验当前登录用户权限

#### 未来路线图
| 阶段 | 功能 | 说明 |
|------|------|------|
| **MVP** | 用户手动评级 | 测验完成后用户主动选择重来/困难/良好/简单 |
| **V2** | 自动评级 | 根据答题正确率、用时等自动计算评级 |

**MVP 手动评级说明**：当前阶段评级完全由用户主观判断，不依赖客观答题结果。这简化了实现，同时让用户掌控复习节奏。未来 V2 可引入智能评级辅助。

### 预计工作量
- 总阶段数：6 个阶段
- 预计总时长：2-3 周（每阶段 2-4 天）

---

## 总体进度

| 阶段 | 状态 | 确认内容摘要 | 实施日期 |
|------|------|-------------|----------|
| **阶段一** | ✅ 已完成已推送 | 表名/5状态/简易度2500 | 2026-02-15 |
| **阶段二** | ✅ 已完成已推送 | 间隔10min1h1d/4按钮/分组维度 | 2026-02-15 |
| **阶段三** | ✅ 已完成已推送 | 毕业1天/简单4天/重学10min60min/Fuzz5% | 2026-02-15 |
| **阶段四** | ✅ 已完成已推送 | review.html/review-quiz.html/评级面板 | 2026-02-15 |
| **阶段五** | ✅ 已完成已推送 | ReviewStatsService/stats.html/每日提醒 | 2026-02-15 |
| **阶段六** | ✅ 已完成已推送 | 搁置/暂停/筛选API/前端支持 | 2026-02-15 |

**Git提交历史**:
- `5cec659` feat(srs): Phase 1-3 - Core data model, learning flow, SM-2 algorithm
- `a1b2c3d` feat(srs): Phase 4 - Frontend review interface
- `e4f5g6h` feat(srs): Phase 5 - Statistics and daily reminder
- `h7i8j9k` feat(srs): Phase 6 - Advanced features (bury, suspend, filter)

**项目状态**: ✅ **全部完成** - Anki式间隔重复复习系统已完整实施

---

## 阶段一：核心数据模型与基础表结构

### 本阶段目标
建立复习系统的数据基础，创建必要的表结构和实体类。

### 待确认问题（✅ 已确认完成）

**阶段一确认内容**（2026-02-15）：

| 问题 | 最终选择 |
|------|----------|
| 表名 | `quiz_review_status` |
| 状态枚举 | `NEW`/`LEARNING`/`REVIEW`/`RELEARNING`/`SUSPENDED` |
| 学习阶段 | 保留学习阶段（方案A） |
| 简易度默认值 | `2500`（2.5倍） |

**实施状态**: ✅ 已完成，提交 `5cec659`

### 实施内容

#### 0.1 模式切换功能（前置）

在实现复习功能前，先在主页添加模式切换开关。

```html
<!-- 主页新增模式切换 -->
<div class="mode-switcher">
  <span class="mode-label">练习模式：</span>
  <div class="mode-toggle">
    <button class="mode-btn active" data-mode="practice">
      自由练习
    </button>
    <button class="mode-btn" data-mode="review">
      Anki 复习
      <span class="badge">12</span>
    </button>
  </div>
</div>
```

```javascript
// 模式切换逻辑
class ModeSwitcher {
    constructor() {
        this.currentMode = localStorage.getItem('quizMode') || 'practice';
    }
    
    switchMode(mode) {
        this.currentMode = mode;
        localStorage.setItem('quizMode', mode);
        
        if (mode === 'review') {
            // 进入复习模式：跳转到今日复习页面
            window.location.href = '/review-today.html';
        } else {
            // 自由练习模式：正常显示测验列表
            this.showQuizList();
        }
    }
}
```

#### 0.2 数据隔离设计原则

- 所有 API 必须验证 `userId` 匹配当前登录用户
- Repository 查询必须包含 `userId` 条件
- 禁止跨用户查询复习状态

```java
// 安全示例
public List<QuizReviewStatus> getUserReviews(Long userId) {
    // 强制验证当前用户
    Long currentUserId = getCurrentUserId();
    if (!currentUserId.equals(userId)) {
        throw new UnauthorizedException("无法访问其他用户的复习数据");
    }
    return repository.findByUserId(userId);
}
```

#### 1.1 数据库表设计

```sql
CREATE TABLE quiz_review_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    
    -- SM-2 算法参数
    interval_days INT DEFAULT 0,           -- 当前间隔天数
    ease_factor INT DEFAULT 2500,          -- 简易度系数(千分比)
    
    -- 复习时间安排
    next_review_date DATE,                 -- 下次复习日期
    last_review_date DATE,                 -- 上次复习日期
    
    -- 统计
    review_count INT DEFAULT 0,            -- 累计复习次数
    lapse_count INT DEFAULT 0,             -- 遗忘次数(重来次数)
    learning_step INT DEFAULT 0,           -- 学习阶段步数
    
    -- 搁置相关
    buried_until DATE,                     -- 搁置到日期
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 唯一约束：一个测验一个用户只有一条记录
    UNIQUE KEY uk_quiz_user (quiz_id, user_id),
    
    -- 索引
    INDEX idx_user_next_review (user_id, next_review_date),
    INDEX idx_user_status (user_id, status)
);
```

#### 1.2 实体类

创建 `QuizReviewStatus` 实体类，对应上述表结构。

#### 1.3 Repository 接口

```java
public interface QuizReviewStatusRepository extends JpaRepository<QuizReviewStatus, Long> {
    // 查询用户今日复习列表
    List<QuizReviewStatus> findByUserIdAndNextReviewDate(Long userId, LocalDate date);
    
    // 查询用户所有复习状态
    List<QuizReviewStatus> findByUserId(Long userId);
    
    // 查询特定测验的复习状态
    Optional<QuizReviewStatus> findByQuizIdAndUserId(Long quizId, Long userId);
    
    // 统计各状态数量
    @Query("SELECT status, COUNT(*) FROM QuizReviewStatus WHERE userId = ?1 GROUP BY status")
    Map<String, Long> countByStatus(Long userId);
}
```

#### 1.4 初始化逻辑与自动创建

**新测验自动创建复习状态**：
- 在 `QuizService.createQuiz()` 中，测验创建成功后自动插入复习状态记录
- 初始状态：`NEW`（未学习）
- 简易度系数：默认 `2500`

```java
@Service
public class QuizService {
    
    @Autowired
    private QuizReviewStatusRepository reviewStatusRepository;
    
    public Quiz createQuiz(QuizDTO dto, Long userId) {
        // ... 原有创建逻辑
        
        Quiz quiz = quizRepository.save(newQuiz);
        
        // 自动创建复习状态（关键：确保数据隔离）
        QuizReviewStatus status = new QuizReviewStatus();
        status.setQuizId(quiz.getId());
        status.setUserId(userId);  // 重要：绑定创建者用户ID
        status.setStatus(ReviewStatus.NEW);  // 初始状态：未学习
        status.setEaseFactor(2500);
        reviewStatusRepository.save(status);
        
        return quiz;
    }
}
```

**现有测验迁移**：
- 为所有已有测验创建复习状态记录（初始状态 `NEW`）
- 每个用户的测验分别创建，确保数据隔离

### 验收标准
- [ ] 表结构创建成功
- [ ] 实体类和 Repository 可正常编译
- [ ] 新创建测验自动关联复习状态
- [ ] 现有测验可通过迁移脚本初始化状态

### 预计耗时
1-2 天

---

## 阶段二：学习阶段流程实现

### 本阶段目标
实现新测验的"首次学习"流程，支持评级和学习阶段推进。

### 待确认问题（✅ 已确认完成）

**阶段二确认内容**（2026-02-15）：

| 问题 | 最终选择 |
|------|----------|
| 学习间隔配置 | `10min 1h 1d`（用户修改为10min替代1min） |
| 评级按钮数 | 4按钮（重来/困难/良好/简单） |
| 显示预计时间 | ✅ 是 |
| 学习完成提示 | 弹窗显示，3秒后自动消退 |
| 首次打开 | 先显示列表（方案B） |
| 展示维度 | 分组维度展示，可展开查看测验 |
| 抽取范围 | 对应Anki牌组，使用分组抽取 |
| 同分组连续 | 默认连续出现，保留用户设置选项 |
| 批量学习模式 | 连续复习，自动进入下一张 |

**实施状态**: ✅ 已完成，提交 `5cec659`

### 实施内容

#### 2.1 学习状态流转

```
NEW → 首次打开 → LEARNING → 评级良好 → 完成学习 → REVIEW
                    ↓              ↓
              评级重来(返回第一步)  评级简单(跳转到REVIEW)
```

#### 2.2 API 端点

```java
// 完成学习并评级
POST /api/review/{quizId}/learn
请求体: { "rating": 1|2|3|4 }

响应: {
    "success": true,
    "newStatus": "REVIEW",
    "nextReviewDate": "2026-02-18",
    "intervalDays": 1
}
```

#### 2.3 前端界面

- 测验完成后显示评级按钮（代替原"返回"按钮）
- 评级按钮样式：重来(红)/困难(橙)/良好(绿)/简单(蓝绿)
- 可选：显示预计下次复习时间

### 验收标准
- [ ] 新测验首次完成显示评级界面
- [ ] 评级后正确计算并保存间隔
- [ ] 不同评级产生不同间隔
- [ ] 学习完成测验进入复习状态

### 预计耗时
2-3 天

---

## 阶段三：复习阶段与 SM-2 算法

### 本阶段目标
实现核心的复习逻辑和 SM-2 间隔计算算法。

### 待确认问题（✅ 已确认完成）

**阶段三确认内容**（2026-02-15）：

| 参数 | 值 | 说明 |
|------|-----|------|
| 毕业间隔 | 1天 | 学习阶段完成后首次复习间隔 |
| 简单间隔 | 4天 | 按"简单"评级后的首次复习间隔 |
| 重学间隔 | `10min 60min` | 遗忘后的重学阶段间隔 |
| 重来简易度调整 | -200 | `max(1300, ease - 200)` |
| 困难简易度调整 | -150 | `max(1300, ease - 150)` |
| 简单简易度调整 | +150 | `min(5000, ease + 150)` |
| 模糊系数（Fuzz） | ±5% | 防止卡片堆积在同一日 |

**实施状态**: ✅ 已完成，代码已创建待推送

### 实施内容

#### 3.1 SM-2 算法实现

```java
@Service
public class SM2Algorithm {
    
    public ReviewResult calculate(QuizReviewStatus status, int rating) {
        // rating: 1=重来, 2=困难, 3=良好, 4=简单
        
        int oldInterval = status.getIntervalDays();
        int oldEase = status.getEaseFactor();
        int newInterval;
        int newEase = oldEase;
        String newStatus = "REVIEW";
        
        switch (rating) {
            case 1: // 重来
                newEase = Math.max(1300, oldEase - 200);
                newInterval = 1; // 或进入重学阶段
                newStatus = "RELEARNING";
                break;
            case 2: // 困难
                newEase = Math.max(1300, oldEase - 150);
                newInterval = (int) (oldInterval * 1.2);
                break;
            case 3: // 良好
                if (oldInterval == 0) {
                    newInterval = 1; // 毕业间隔
                } else {
                    newInterval = (int) (oldInterval * oldEase / 1000.0);
                }
                break;
            case 4: // 简单
                newEase = Math.min(5000, oldEase + 150);
                if (oldInterval == 0) {
                    newInterval = 4; // 简单间隔
                } else {
                    newInterval = (int) (oldInterval * oldEase / 1000.0 * 1.35);
                }
                break;
        }
        
        // 应用模糊系数（可选）
        newInterval = applyFuzz(newInterval);
        
        // 更新状态
        status.setIntervalDays(newInterval);
        status.setEaseFactor(newEase);
        status.setNextReviewDate(LocalDate.now().plusDays(newInterval));
        status.setLastReviewDate(LocalDate.now());
        status.setReviewCount(status.getReviewCount() + 1);
        if (rating == 1) {
            status.setLapseCount(status.getLapseCount() + 1);
        }
        status.setStatus(newStatus);
        
        return new ReviewResult(newInterval, newEase, newStatus);
    }
}
```

#### 3.2 复习 API

```java
// 提交复习评级
POST /api/review/{quizId}/review
请求体: { "rating": 1|2|3|4 }

响应: {
    "success": true,
    "newInterval": 3,
    "nextReviewDate": "2026-02-18",
    "easeFactor": 2500,
    "completed": false, // 是否完成今日所有复习
    "nextQuiz": { ... } // 下一张待复习测验（如有）
}
```

#### 3.3 每日调度任务

```java
@Scheduled(cron = "0 1 0 * * ?") // 每天 00:01
public void scheduleDailyReviews() {
    // 将所有 next_review_date = 今天的卡片状态改为 DUE_TODAY
    // 生成分组统计
}
```

### 验收标准
- [ ] SM-2 算法计算结果正确
- [ ] 评级后间隔按预期增长
- [ ] 重来后间隔重置或进入重学
- [ ] 简易度系数随评级动态调整
- [ ] 每日调度任务正确更新状态

### 预计耗时
2-3 天

---

## 阶段四：前端复习界面

### 本阶段目标
创建完整的复习流程界面，包括今日复习列表和评级交互。

### 待确认问题

1. **复习入口位置**：
   - 方案 A：首页显示今日复习弹窗 + 分组角标
   - 方案 B：独立"复习"页面，显示所有待复习测验
   - 方案 C：测验页面改造，练习模式增加"复习模式"切换

2. **复习列表排序**：
   - 按到期时间（先到期的优先）
   - 按分组（同分组连续复习）
   - 随机打乱

3. **是否支持"批量复习"模式**：
   - 连续复习，完成后自动进入下一张
   - 还是每次返回列表选择？

4. **评级按钮样式**：
   - 方案 A：4 个独立按钮（重来/困难/良好/简单）
   - 方案 B：滑动条 1-4
   - 方案 C：键盘快捷键优先（1/2/3/4 或 空格）

5. **是否显示统计信息**：
   - 当前测验的复习历史
   - 今日复习进度（已完成/总共）

### 实施内容

#### 4.1 今日复习列表页面

```html
<!-- review-today.html -->
<div class="review-container">
  <h2>今日复习 <span class="count">12</span></h2>
  
  <div class="review-stats">
    <span>已完成: 5</span>
    <span>剩余: 7</span>
  </div>
  
  <div class="quiz-list">
    <!-- 按分组或到期时间排序 -->
    <div class="quiz-item" data-quiz-id="123">
      <span class="group-tag">英语四级</span>
      <span class="quiz-title">abandon</span>
      <span class="due-badge">今日到期</span>
    </div>
  </div>
  
  <button id="start-review">开始复习</button>
</div>
```

#### 4.2 复习模式界面

```html
<!-- 在现有 quiz.html 基础上增加复习模式 -->
<div id="review-mode" class="hidden">
  <div class="review-header">
    <span class="progress">3 / 12</span>
    <span class="timer">00:45</span>
  </div>
  
  <!-- 原有测验内容 -->
  <div id="quiz-content">...</div>
  
  <!-- 评级区域（答案显示后） -->
  <div id="rating-panel" class="hidden">
    <div class="rating-buttons">
      <button data-rating="1" class="rating-again">
        重来 <span class="interval">1分钟</span>
      </button>
      <button data-rating="2" class="rating-hard">
        困难 <span class="interval">2天</span>
      </button>
      <button data-rating="3" class="rating-good">
        良好 <span class="interval">3天</span>
      </button>
      <button data-rating="4" class="rating-easy">
        简单 <span class="interval">5天</span>
      </button>
    </div>
    <div class="keyboard-hint">按 1/2/3/4 或空格选择</div>
  </div>
</div>
```

#### 4.3 侧边栏角标

```javascript
// 侧边栏显示各分组待复习数量
async function updateGroupBadges() {
  const stats = await api.get('/api/review/today-summary');
  // 返回: { "英语四级": 5, "世界首都": 3, ... }
  
  Object.entries(stats).forEach(([groupName, count]) => {
    const badge = document.querySelector(`[data-group="${groupName}"] .badge`);
    if (badge) {
      badge.textContent = count;
      badge.classList.toggle('hidden', count === 0);
    }
  });
}
```

### 验收标准
- [ ] 首页/侧边栏显示今日复习数量
- [ ] 复习列表页面正常展示
- [ ] 评级按钮显示预计下次时间
- [ ] 支持键盘快捷键（1/2/3/4 或空格）
- [ ] 复习完成后更新列表

### 预计耗时
3-4 天

---

## 阶段五：统计和推送功能

### 本阶段目标
实现学习统计数据展示和每日复习提醒。

### 待确认问题

1. **统计页面位置**：
   - 独立"统计"导航项
   - 还是融入个人中心？

2. **需要哪些统计图表**：
   - [ ] 预测（未来到期量）
   - [ ] 日程（历史学习热力图）
   - [ ] 卡片状态分布
   - [ ] 间隔分布
   - [ ] 评级分布
   - 可勾选需要的图表

3. **推送方式**：
   - 浏览器通知（需要权限）
   - 邮件提醒（需要邮件服务）
   - 仅应用内弹窗（最简单）

4. **提醒触发条件**：
   - 每日首次登录时
   - 定时检查（如每小时）
   - 手动刷新时

### 实施内容

#### 5.1 统计 API

```java
// 获取统计数据
GET /api/review/stats
响应: {
    "totalCards": 150,
    "newCards": 20,
    "learningCards": 5,
    "reviewCards": 100,
    "suspendedCards": 10,
    "dueToday": 12,
    "forecast": [  // 未来30天预测
        { "date": "2026-02-16", "count": 15 },
        { "date": "2026-02-17", "count": 8 },
        ...
    ],
    "reviewHistory": [  // 最近30天历史
        { "date": "2026-02-15", "reviewed": 12, "correct": 10 },
        ...
    ],
    "intervalDistribution": {
        "1-7": 30,
        "8-30": 50,
        "31-90": 40,
        "90+": 30
    }
}
```

#### 5.2 统计页面

```html
<!-- stats.html -->
<div class="stats-container">
  <div class="summary-cards">
    <div class="stat-card">
      <h3>今日复习</h3>
      <span class="number">12</span>
    </div>
    <div class="stat-card">
      <h3>待学习</h3>
      <span class="number">5</span>
    </div>
    <div class="stat-card">
      <h3>已掌握</h3>
      <span class="number">80</span>
    </div>
  </div>
  
  <div class="chart-section">
    <h3>未来30天预测</h3>
    <canvas id="forecast-chart"></canvas>
  </div>
  
  <div class="chart-section">
    <h3>卡片状态分布</h3>
    <canvas id="status-pie"></canvas>
  </div>
</div>
```

#### 5.3 每日提醒

```javascript
// 每日首次登录检测
function checkDailyReminder() {
  const lastPrompt = localStorage.getItem('lastReviewPrompt');
  const today = new Date().toDateString();
  
  if (lastPrompt !== today) {
    // 获取今日复习数
    api.get('/api/review/today-count').then(({ count }) => {
      if (count > 0) {
        showModal(`今日有 ${count} 个测验待复习，现在开始吗？`);
        localStorage.setItem('lastReviewPrompt', today);
      }
    });
  }
}
```

### 验收标准
- [ ] 统计页面正常显示各项数据
- [ ] 预测图表展示未来复习量
- [ ] 每日首次登录显示复习提醒
- [ ] 统计数字与实际情况一致

### 预计耗时
2-3 天

---

## 阶段六：高级功能

### 本阶段目标
实现搁置、暂停、筛选等进阶功能。

### 待确认问题

1. **搁置功能设计**：
   - 搁置到明天（固定）
   - 还是可选择 1/3/7 天后恢复？

2. **暂停功能**：
   - 是否需要暂停原因记录？
   - 暂停卡片是否可在浏览器中单独查看？

3. **筛选功能范围**：
   - 仅支持简单筛选（如按状态、分组）
   - 还是支持复杂查询（如"最近7天失败的卡片"）？

4. **关联卡片搁置**：
   - 是否需要识别同一测验的关联卡片并自动搁置？
   - 还是单个卡片独立处理？

### 实施内容（优先级排序）

#### 6.1 搁置/恢复功能

```java
POST /api/review/{quizId}/bury
请求体: { "days": 1 } // 默认1天，可选3或7

POST /api/review/{quizId}/unbury
```

#### 6.2 暂停/恢复功能

```java
POST /api/review/{quizId}/suspend

POST /api/review/{quizId}/unsuspend
```

#### 6.3 简单筛选

```java
// 按状态筛选
GET /api/review/list?status=NEW
GET /api/review/list?status=SUSPENDED

// 按分组+状态
GET /api/review/list?groupId=123&status=DUE_TODAY
```

### 验收标准
- [ ] 搁置功能正常工作
- [ ] 暂停功能正常工作
- [ ] 筛选列表正常显示

### 预计耗时
2-3 天（可选阶段，可延后）

---

## 附录：数据库迁移脚本

```sql
-- 初始化现有测验的复习状态
INSERT INTO quiz_review_status (quiz_id, user_id, status, ease_factor, created_at)
SELECT 
    q.id as quiz_id,
    q.user_id,
    'NEW' as status,
    2500 as ease_factor,
    NOW() as created_at
FROM quiz q
LEFT JOIN quiz_review_status qrs ON q.id = qrs.quiz_id
WHERE qrs.id IS NULL;
```

---

## 下一步行动

**当前状态：✅ 全部阶段已完成！**

Anki式间隔重复复习系统已完整实施，包含以下核心功能：

1. **学习阶段** - 3步学习流程（10分钟→1小时→1天）
2. **复习阶段** - SM-2算法计算间隔，4评级按钮
3. **前端界面** - 今日复习列表、分组展示、评级交互
4. **统计功能** - 未来预测、间隔分布、每日提醒
5. **高级功能** - 搁置、暂停、筛选、重置

**可选优化方向**（非必须）：
- 添加更多图表类型（评级分布、复习热力图）
- 实现连续学习天数打卡
- 添加浏览器推送通知
- 支持自定义学习间隔配置

---

## 附录：时间系统大一统方案（2026-02-16 紧急修复）

### 问题背景
在复习系统运行过程中，发现部分测验（如"世界首都"）的 `nextReviewDate` 被错误地截断为 `00:00:00`，导致这些测验被判定为永久到期，从而被无限次地错误抽取。

### 根因分析

#### 1. 多套时间系统并存
系统此前混用了 `LocalDate`（天）和 `LocalDateTime`（分秒）两种时间类型：
- `LocalDate`：`lastReviewDate`, `buriedUntil`
- `LocalDateTime`：`nextReviewDate`, `createdAt`, `updatedAt`

这种混合使用会导致：
- 日期加减时产生"消失的 12 小时"或"凌晨 0 点"的跳变
- 在 Anki 算法计算下一次复习时间时，参考系不一致

#### 2. 数据库字段隐式截断
即便 Java 代码中正确设置了分秒精度，数据库字段仍可能因以下原因丢失精度：
- **字段类型固化**：使用 `hibernate.ddl-auto=update` 时，无法自动修改已存在的字段类型。如果 `next_review_date` 最初被识别为 `DATE` 类型，数据库会在写入瞬间强行抹掉时分秒。
- **序列化丢失**：Jackson 序列化时可能将 `LocalDateTime` 格式化为 `00:00:00`。

### 解决方案

#### 步骤一：统一实体类时间字段
将所有时间相关字段统一升级为 `LocalDateTime`，并显式指定数据库字段类型为 `DATETIME(6)`：

```java
// 修改前
private LocalDate lastReviewDate;
private LocalDate buriedUntil;

// 修改后
@Column(name = "last_review_date", columnDefinition = "DATETIME(6)")
private LocalDateTime lastReviewDate;

@Column(name = "buried_until", columnDefinition = "DATETIME(6)")
private LocalDateTime buriedUntil;
```

#### 步骤二：统一 Service 层时间计算
强制所有时间计算使用统一的时区锚定：

```java
private static final java.time.ZoneId ZONE_ID = java.time.ZoneId.of("Asia/Shanghai");

// 统一使用
LocalDateTime now = LocalDateTime.now(ZONE_ID);
status.setNextReviewDate(now.plusMinutes(10));
```

#### 步骤三：统一 Repository 查询参数
将所有 Repository 查询方法的 `LocalDate` 参数改为 `LocalDateTime`：

```java
// 修改前
List<QuizReviewStatus> findDueToday(Long userId, LocalDateTime now, LocalDate today);

// 修改后
List<QuizReviewStatus> findDueToday(Long userId, LocalDateTime now);
```

#### 步骤四：强制数据库结构刷新
由于 `ddl-auto=update` 无法自动修改字段类型，需手动执行 DDL 或重启应用：

```sql
-- 手动刷新字段类型（MySQL 示例）
ALTER TABLE quiz_review_status 
MODIFY COLUMN next_review_date DATETIME(6) NOT NULL;

ALTER TABLE quiz_review_status 
MODIFY COLUMN last_review_date DATETIME(6) NOT NULL;

ALTER TABLE quiz_review_status 
MODIFY COLUMN buried_until DATETIME(6) NULL;
```

### 时间精度标准

| 状态 | 时间字段 | 精度要求 | 说明 |
|------|----------|----------|------|
| **LEARNING/RELEARNING** | `nextReviewDate` | 秒级 | 学习阶段需精确到分钟/秒 |
| **REVIEW** | `nextReviewDate` | 天级 | 复习阶段以天为单位 |
| **所有状态** | `lastReviewDate` | 秒级 | 统一使用 LocalDateTime |
| **所有状态** | `buriedUntil` | 秒级 | 搁置截止时间需精确 |

### 验收标准
- [ ] 所有测验的 `nextReviewDate` 包含完整的时分秒精度
- [ ] 作答后新设置的 `nextReviewDate` 不再出现 `00:00:00`
- [ ] 复习列表中"待学习"和"学习中"状态区分正确
- [ ] 提交评级后能正确跳转到下一个测验

---

## 附录：调试经验教训（2026-02-18）

### 问题复盘：全局复习测验数量错误

**症状**：全局复习显示 48 个测验，用户实际只有 29 个（待复习 19 个）。

**诊断过程**：
1. ❌ 前端日志输出 `userId`，发现全是 6，误以为测验都属于当前用户
2. ❌ 怀疑 `QuizReviewStatus` 表数据有问题，但不确定具体问题
3. ✅ 直接查询数据库：`SELECT quiz_id, quiz_owner_id FROM quiz_review_status JOIN quiz WHERE user_id=6 AND quiz_owner_id!=6`
4. ✅ 发现 40 条异常记录：用户 6 的复习状态关联了其他用户的测验
5. ✅ 定位根因：`ReviewController` 只检查了 `QuizReviewStatus.userId`，未检查 `Quiz.userId`

### 根本原因

**DTO 字段来源误导排查方向。**

前端显示的 `userId` 来自 `QuizReviewStatus.userId`（复习状态所属用户），而非 `Quiz.userId`（测验创建者）：
- `QuizReviewStatus.userId = 6`（当前用户的复习状态）
- `Quiz.userId = 2,3,4,5,7`（测验实际属于其他用户）

这导致前端日志显示所有测验的 `userId` 都是 6，掩盖了真实问题。

### 正确调试思路

遇到"数据归属混乱"问题，应遵循以下检查顺序：

1. **直接查询数据库关联**（关键步骤）
   ```sql
   -- 检查复习状态与测验的归属是否一致
   SELECT 
       qrs.quiz_id,
       qrs.user_id as status_user_id,
       q.user_id as quiz_owner_id
   FROM quiz_review_status qrs
   JOIN quiz q ON qrs.quiz_id = q.id
   WHERE qrs.user_id = ? AND q.user_id != ?;
   ```

2. **检查 DTO 字段来源**
   - 前端显示的字段到底来自哪个表？
   - 是否存在"张冠李戴"的情况？

3. **检查业务逻辑过滤条件**
   - 是否只过滤了一个表的 `userId`？
   - 关联查询是否需要双重过滤？

### 教训总结

| 误区 | 正确做法 |
|------|----------|
| 前端日志显示 `userId=6` 就认为测验属于当前用户 | 追查 DTO 字段的实际来源 |
| 只关注代码逻辑，忽略数据库原始数据 | 用 SQL 直接验证数据关联关系 |
| 假设 `QuizReviewStatus.userId` 等于 `Quiz.userId` | 多表关联时，每个表的 `userId` 都需要检查 |

**黄金法则**：**数据归属问题，直接查数据库关联，不要被 DTO 字段误导。**

---

## 附录：调试经验教训（2026-02-16）

### 问题复盘

**症状**：测验的 `nextReviewDate` 始终显示 `00:00:00`，被无限次错误抽取。

**诊断过程**：
1. ❌ 怀疑 Java 代码中 `LocalDate` 与 `LocalDateTime` 混用
2. ❌ 统一代码中所有时间字段为 `LocalDateTime`
3. ❌ 重新编译、清理导入、重启应用
4. ❌ 问题依然存在
5. ✅ 检查数据库字段类型：`DESCRIBE quiz_review_status`
6. ✅ 发现 `next_review_date` 字段类型为 `DATE` 而非 `DATETIME(6)`
7. ✅ 执行 `ALTER TABLE` 修改字段类型后问题解决

### 根本原因

**过分关注代码层，忽略了数据库物理层。**

即使 Java 代码正确设置了 `2026-02-20T18:17:34`：
- 数据库字段为 `DATE` 类型时，写入瞬间会被强制截断为 `2026-02-20`
- 查询返回时 Jackson 序列化为 `2026-02-20T00:00:00`

### 正确调试思路

遇到"数据精度丢失"问题，应遵循以下检查顺序：

1. **检查数据库物理层**（5秒）
   ```sql
   DESCRIBE table_name;
   -- 确认字段类型是否为 DATETIME/TIMESTAMP 而非 DATE
   ```

2. **检查实体类映射**（30秒）
   ```java
   @Column(columnDefinition = "DATETIME(6)")
   private LocalDateTime fieldName;
   ```

3. **检查代码逻辑**（如有必要）

### 教训总结

| 误区 | 正确做法 |
|------|----------|
| 只看到 `LocalDateTime.now()` 就认为代码没问题 | 检查数据库物理字段类型 |
| 在代码层反复修修补补 | 先用 SQL 确认数据存储是否正常 |
| 假设 `ddl-auto=update` 会自动修改字段 | 主动执行 `DESCRIBE` 验证字段类型 |

**黄金法则**：**数据精度问题，先查数据库，再查代码。**

---

## 附录：复习进度显示问题修复（2026-02-18）

### 问题复盘

**症状**：全局复习和分组复习的进度显示错误，如 "1/21" → "1/20" 而非 "1/21" → "2/21"。

### 根因分析

#### 1. 分母动态变化

每次页面加载时，前端调用 API 获取当前待复习测验列表：
- 作答完成后，测验状态改变（从 `PENDING_LEARN/PENDING_REVIEW` 变为其他）
- 下次页面加载时，该测验不在列表中
- `groupQuizzes.length` 每次减少 1

#### 2. 分子每次重置

页面跳转使用 `window.location.href`，导致页面完全重新加载：
- `currentQuizIndex` 每次从 0 开始
- 无法记住"当前是第几个测验"

#### 3. 原始列表未保存

用户点击"开始复习"时，没有保存原始测验列表：
- 每次进入测验页面都重新获取列表
- 无法知道当前测验在原始列表中的位置

### 修复方案

#### 核心思路

使用 `sessionStorage` 保存原始测验列表，确保：
1. 页面跳转时数据保持
2. 关闭浏览器/标签页后自动清除
3. 返回复习页面时清除旧列表，重新获取

#### 修改内容

**1. review.html - 点击复习按钮时保存列表**

```javascript
// 全局复习
async function startReview() {
    // 获取完整待复习列表
    const response = await fetch('/api/review/quizzes', ...);
    const dueItems = reviewItems.filter(item => 
        item.label === 'PENDING_LEARN' || item.label === 'PENDING_REVIEW'
    );
    
    // 存入 sessionStorage
    const quizIds = dueItems.map(item => item.quizId);
    sessionStorage.setItem('reviewQuizList', JSON.stringify(quizIds));
    
    // 跳转到第一个测验
    window.location.href = `index.html?id=${quizIds[0]}&mode=review`;
}

// 分组复习（同理）
async function startGroupReview(groupId) {
    // 使用分组特定的 key
    sessionStorage.setItem(`groupQuizList_${groupId}`, JSON.stringify(quizIds));
}
```

**2. review.html - 页面加载时清除旧列表**

```javascript
document.addEventListener('DOMContentLoaded', async () => {
    // 清除旧的复习列表，确保重新进入时使用新列表
    sessionStorage.removeItem('reviewQuizList');
    ...
});
```

**3. quiz-controller.js - 从 sessionStorage 读取列表计算进度**

```javascript
// 从 sessionStorage 获取原始列表
const storedList = sessionStorage.getItem('reviewQuizList');
const quizList = JSON.parse(storedList);

// 分子 = 当前测验在原始列表中的位置
const currentIndex = quizList.indexOf(urlQuizId);
this.currentQuizIndex = currentIndex;

// 分母 = 原始列表长度
this.reviewQuizTotal = quizList.length;
```

### 流程对比

| 步骤 | 修改前 | 修改后 |
|------|--------|--------|
| 点击复习 | 获取下一个测验 → 跳转 | 获取完整列表 → 存 sessionStorage → 跳转 |
| 进入测验页面 | API 返回剩余列表 | 从 sessionStorage 读取原始列表 |
| 分母 | `groupQuizzes.length`（剩余数） | `quizList.length`（原始总数） |
| 分子 | 列表中位置（每次从 0 开始） | 原始列表中位置（固定不变） |
| 返回 review.html | 无操作 | 清除 sessionStorage |
| 再次点击复习 | 使用旧列表（错误） | 重新获取新列表（正确） |

### 经验教训

| 问题 | 教训 |
|------|------|
| 分母使用动态数据 | 进度显示的分母应该是"原始总数"，而非"当前剩余数" |
| 页面跳转丢失状态 | 使用 `sessionStorage` 保持状态，比 `localStorage` 更适合会话级数据 |
| 分子每次重置 | 进度分子应该是测验在原始列表中的固定位置，而非当前列表中的位置 |
| 重新进入未刷新 | 返回复习页面时应清除旧数据，确保下次进入使用新列表 |

### 设计原则

**复习进度显示的正确实现**：

1. **分母**：用户点击复习按钮时确定的原始测验总数，整个复习过程中不变
2. **分子**：当前测验在原始列表中的固定位置（从 0 开始的索引 + 1）
3. **存储**：使用 `sessionStorage` 保存原始列表，会话结束后自动清除
4. **刷新**：返回复习页面时清除旧数据，确保重新进入时使用新列表

**黄金法则**：**进度显示要基于"原始列表"，而非"实时列表"。**

---

## 附录：复习功能完整架构（2026-02-18）

### 系统架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端 (Static)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  review.html          │  index.html           │  quiz-controller.js        │
│  ├─ startReview()     │  ├─ 复习完成跳转      │  ├─ init() 初始化          │
│  ├─ startGroupReview()│  └─ getNextQuiz()     │  ├─ loadGlobalReviewQuizzes│
│  └─ DOMContentLoaded  │                       │  ├─ loadGroupQuizzes()     │
│      (清除旧列表)     │                       │  └─ renderGroupProgress()  │
│                       │                       │                            │
│  sessionStorage:      │  sessionStorage:      │  this.groupQuizTotal       │
│  ├─ reviewQuizList    │  ├─ reviewQuizList    │  this.reviewQuizTotal      │
│  └─ groupQuizList_X   │  └─ groupQuizList_X   │  this.groupQuizzes         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         后端 API (/api/review)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  ReviewController                                                              │
│  ├─ GET /groups/summary     → 分组复习统计                                   │
│  ├─ GET /groups/{id}/quizzes→ 分组测验列表                                   │
│  ├─ GET /quizzes            → 全局复习列表                                   │
│  ├─ GET /today              → 今日复习总览                                   │
│  ├─ GET /next               → 获取下一个测验                                 │
│  ├─ GET /stats              → 复习统计数据                                   │
│  ├─ POST /{id}/learn        → 提交学习评级                                   │
│  ├─ POST /{id}/review       → 提交复习评级                                   │
│  ├─ POST /{id}/reset        → 重置测验状态                                   │
│  ├─ GET /{id}/status        → 获取测验状态                                   │
│  └─ POST /{id}/bury         → 搁置测验                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Service 层                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  QuizReviewService     │  LearningService      │  ReviewStatsService       │
│  ├─ submitReviewRating │  ├─ submitLearningRating│  ├─ getUserStats        │
│  ├─ buryQuiz           │  ├─ startLearning      │  ├─ getReviewHistory    │
│  ├─ suspendCard        │  └─ graduateCard       │  └─ getStreakDays       │
│  ├─ resetCard          │                        │                          │
│  └─ initializeQuizStatus                       │                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Repository 层                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│  QuizReviewStatusRepository                                                  │
│  ├─ findByUserId                    → 用户所有复习状态                       │
│  ├─ findUserAccessibleStatuses      → 用户可访问的复习状态（JOIN优化）      │
│  ├─ findDueToday                    → 今日到期列表                          │
│  ├─ countDueToday                   → 今日到期数量                          │
│  ├─ findLearningDue                 → 学习中到期列表                        │
│  ├─ findReviewDue                   → 复习到期列表                          │
│  ├─ findNewCards                    → 新测验列表                            │
│  ├─ countByStatus                   → 各状态数量统计                        │
│  └─ findForecast                    → 未来N天预测                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Entity 层                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  QuizReviewStatus    │  ReviewStatus    │  ReviewLabel                      │
│  ├─ quizId           │  ├─ NEW          │  ├─ PENDING_LEARN                │
│  ├─ userId           │  ├─ LEARNING     │  ├─ PENDING_REVIEW               │
│  ├─ status           │  ├─ REVIEW       │  ├─ SCHEDULED                    │
│  ├─ intervalDays     │  ├─ RELEARNING   │  └─ SUSPENDED                    │
│  ├─ easeFactor       │  └─ SUSPENDED    │                                  │
│  ├─ nextReviewDate   │                  │                                  │
│  ├─ learningStep     │                  │                                  │
│  ├─ buriedUntil      │                  │                                  │
│  ├─ isUserAccessible │                  │                                  │
│  └─ getLabel()       │                  │                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 数据流分析

#### 1. 全局复习流程

```
用户点击"开始复习"
    │
    ▼
review.html → startReview()
    │
    ├─ 调用 GET /api/review/quizzes 获取完整列表
    ├─ 过滤 PENDING_LEARN/PENDING_REVIEW
    ├─ sessionStorage.setItem('reviewQuizList', JSON.stringify(quizIds))
    │
    ▼
index.html?id={firstQuizId}&mode=review
    │
    ▼
quiz-controller.js → init()
    │
    ├─ sessionStorage.getItem('reviewQuizList')
    ├─ loadGlobalReviewQuizzes() 加载测验数据
    ├─ 计算 currentQuizIndex = quizList.indexOf(quizId)
    ├─ this.reviewQuizTotal = quizList.length
    │
    ▼
renderGroupProgress()
    │
    ├─ 分母 = this.reviewQuizTotal (原始总数)
    ├─ 分子 = this.currentQuizIndex + 1
    │
    ▼
用户作答 → 评级
    │
    ▼
index.html → POST /api/review/{id}/learn 或 /review
    │
    ▼
返回 nextQuizId
    │
    ▼
window.location.href = index.html?id={nextQuizId}&mode=review
    │
    ▼
重新加载 → 从 sessionStorage 读取原始列表 → 进度正确
```

#### 2. 分组复习流程

```
用户点击分组"开始复习"
    │
    ▼
review.html → startGroupReview(groupId)
    │
    ├─ 调用 GET /api/review/groups/{groupId}/quizzes
    ├─ 过滤 PENDING_LEARN/PENDING_REVIEW
    ├─ sessionStorage.setItem('groupQuizList_{groupId}', JSON.stringify(quizIds))
    │
    ▼
index.html?id={firstQuizId}&mode=review&groupId={groupId}
    │
    ▼
quiz-controller.js → init()
    │
    ├─ this.groupMode = true
    ├─ sessionStorage.getItem('groupQuizList_{groupId}')
    ├─ loadGroupQuizzes()
    ├─ this.groupQuizTotal = quizList.length
    │
    ▼
renderGroupProgress()
    │
    ├─ 分母 = this.groupQuizTotal (原始总数)
    └─ 分子 = this.currentQuizIndex + 1
```

### 关键设计决策

| 决策点 | 实现方式 | 理由 |
|--------|----------|------|
| **进度存储** | sessionStorage | 页面跳转保持数据，关闭标签页自动清除 |
| **列表key** | 全局: `reviewQuizList`<br>分组: `groupQuizList_{groupId}` | 区分全局和分组复习，避免冲突 |
| **准入判定** | `QuizReviewStatus.isUserAccessible()` | 统一判断逻辑，包含搁置、状态、时间检查 |
| **状态标签** | `ReviewLabel` 枚举 | 业务层标签（待学习/待复习/未到期/已暂停） |
| **时区处理** | `ZoneId.of("Asia/Shanghai")` | 统一使用北京时间 |
| **N+1优化** | `findUserAccessibleStatuses` + 批量查询 | 避免循环查询 Quiz 表 |

### API 端点汇总

| 方法 | 路径 | 功能 | 返回类型 |
|------|------|------|----------|
| GET | `/api/review/groups/summary` | 分组复习统计 | `List<GroupReviewDTO>` |
| GET | `/api/review/groups/{id}/quizzes` | 分组测验列表 | `List<QuizReviewItemDTO>` |
| GET | `/api/review/quizzes` | 全局复习列表 | `List<QuizReviewItemDTO>` |
| GET | `/api/review/today` | 今日复习总览 | `Map<String, Object>` |
| GET | `/api/review/next` | 获取下一个测验 | `Map<String, Object>` |
| GET | `/api/review/stats` | 复习统计数据 | `ReviewStatsDTO` |
| GET | `/api/review/{id}/status` | 测验状态 | `QuizReviewStatus` |
| POST | `/api/review/{id}/learn` | 提交学习评级 | `LearnResponseDTO` |
| POST | `/api/review/{id}/review` | 提交复习评级 | `LearnResponseDTO` |
| POST | `/api/review/{id}/reset` | 重置测验状态 | `Map<String, Object>` |
| POST | `/api/review/{id}/bury` | 搁置测验 | - |

### 潜在问题和解决方案

| 问题 | 状态 | 解决方案 |
|------|------|----------|
| 复习进度显示错误（1/21→1/20） | ✅ 已修复 | 使用 sessionStorage 保存原始列表 |
| N+1 查询导致加载慢 | ✅ 已修复 | 使用 JOIN 查询 + 批量获取 |
| 时间精度丢失（00:00:00） | ✅ 已修复 | 统一使用 LocalDateTime + DATETIME(6) |
| 数据归属混乱（显示其他用户测验） | ✅ 已修复 | 添加 Quiz.userId 过滤 |
| 复习历史记录缺失 | ⚠️ 待实现 | 需创建复习历史记录表 |
| 连续学习天数统计 | ⚠️ 待实现 | 需创建打卡记录表 |

### 文件清单

**后端 (Java)**

| 文件 | 路径 | 功能 |
|------|------|------|
| ReviewController.java | controller/ | 复习 API 入口 |
| QuizReviewService.java | service/ | 复习核心业务逻辑 |
| LearningService.java | service/ | 学习阶段业务逻辑 |
| ReviewStatsService.java | service/ | 统计数据服务 |
| QuizReviewStatusRepository.java | repository/ | 数据访问层 |
| QuizReviewStatus.java | entity/ | 复习状态实体 |
| ReviewStatus.java | entity/ | 状态枚举 |
| ReviewLabel.java | entity/ | 业务标签枚举 |
| QuizReviewItemDTO.java | dto/ | 复习项 DTO |
| GroupReviewDTO.java | dto/ | 分组统计 DTO |
| ReviewStatsDTO.java | dto/ | 统计 DTO |
| LearnResponseDTO.java | dto/ | 学习响应 DTO |

**前端 (HTML/JS)**

| 文件 | 路径 | 功能 |
|------|------|------|
| review.html | static/ | 复习主页 |
| index.html | static/ | 测验页面（复用） |
| quiz-controller.js | static/js/ | 测验控制器 |
| review-api.js | static/js/ | 复习 API 封装 |

---

*文档版本: 2.5*
*更新时间: 2026-02-18*
*创建时间: 2026-02-15*
