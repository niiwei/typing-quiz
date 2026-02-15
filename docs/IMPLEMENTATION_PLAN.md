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

#### 模式切换设计
- **自由测验模式**（现有）：无复习追踪，用于快速练习
- **Anki 复习模式**（新增）：启用 SM-2 算法追踪
- **切换位置**：主页新增模式选择开关，用户可自由切换

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

## 阶段一：核心数据模型与基础表结构

### 本阶段目标
建立复习系统的数据基础，创建必要的表结构和实体类。

### 待确认问题
**请在确认以下问题后再开始实施：**

1. **表名确认**：`quiz_review_status` 是否合适？或偏好 `quiz_srs_data` / `quiz_schedule`？
2. **状态枚举值**：以下状态是否覆盖需求？
   - `NEW` (新测验)
   - `LEARNING` (学习中)
   - `REVIEW` (待复习)
   - `RELEARNING` (重学中)
   - `SUSPENDED` (暂停)
3. **是否保留学习阶段？** 学习阶段会增加复杂度，可考虑简化：
   - 方案 A：保留学习阶段（参考 Anki 的 3 步学习）
   - 方案 B：简化，新测验首次完成即进入复习阶段
4. **简易度系数默认值**：`2500`（2.5倍）是否合适？

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

### 待确认问题

1. **学习阶段设计选择**：
   - **方案 A（完整）**：3 步学习 `1m → 10m → 1d`，完成进入复习
   - **方案 B（简化）**：无学习阶段，首次完成即进入复习（间隔=1d）
   - **方案 C（超简化）**：首次完成后由用户选择"简单/正常"，分别分配不同起始间隔

2. **评级按钮数量**：
   - 4 按钮（重来/困难/良好/简单）- 完整但复杂
   - 3 按钮（重来/良好/简单）- 平衡
   - 2 按钮（重来/良好）- 最简单

3. **是否显示预计下次复习时间**？
   - 显示：用户可预知安排
   - 不显示：简化界面

4. **学习完成后提示**：
   - 显示"下次复习：3天后"确认信息？
   - 静默完成，自动安排？

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

### 待确认问题

1. **初学间隔配置**（仅当选择方案 A 时）：
   ```
   建议：1分钟 10分钟 1天
   是否接受？或自定义？
   ```

2. **毕业间隔**（学习阶段完成后首次复习间隔）：
   - 默认 1 天是否合适？

3. **简单间隔**（按"简单"后的间隔）：
   - 建议 4 天（比毕业间隔长）
   - 是否接受？

4. **简易度系数调整规则**：
   ```
   重来：ease = max(1300, ease - 200)
   困难：ease = max(1300, ease - 150)
   良好：ease 不变
   简单：ease = min(5000, ease + 150)
   ```
   是否接受此调整幅度？

5. **重学阶段设计**：
   - 方案 A：遗忘后进入重学阶段，使用重学间隔（1m 10m），完成后重新毕业
   - 方案 B：遗忘后直接重置间隔为 1 天，无需重学阶段

6. **模糊系数（Fuzz）**：
   - 是否添加小幅随机波动（±5%）防止卡片堆积？

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

**当前状态：等待用户确认阶段一方案**

请审阅**阶段一**的待确认问题（表名、状态枚举、是否保留学习阶段、简易度默认值），告诉我：
1. 是否有需要修改的地方？
2. 确认后我将开始实施阶段一的代码。

---

*文档版本: 1.0*
*创建时间: 2026-02-15*
