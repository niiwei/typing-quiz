# Typing Quiz 更新日志

## [v1.9.0] - 2026-02-23

### 测验分组体系深度重构
- **核心变更**：彻底取消“未分组”状态，所有测验强制归属于特定分组。
- **默认分组机制**：
  - **自动归属**：新创建且未手动指定分组的测验自动归属于“默认分组”。
  - **数据迁移**：通过数据库脚本将现有所有未分组测验迁移至对应用户的“默认分组”。
- **后端架构优化**：
  - **API 简化**：`ReviewController` 移除对特殊字符串 `ungrouped` 的解析逻辑，统一使用 Long 类型 ID。
  - **Repository 增强**：`QuizGroupRepository` 新增 `findByUserIdAndName` 以支持默认分组的精准查找。
  - **Service 逻辑固化**：`QuizService` 在测验持久化流程中增加默认分组强制关联。
- **前端交互重塑**：
  - **UI 清理**：首页 (`home.html`)、测验列表页 (`quizzes.html`)、管理页 (`manage.html`) 全面移除“未分组”虚拟选项。
  - **跳转修复**：修复了复习模式下，原“未分组”测验在评级后意外跳转到全局复习列表的逻辑漏洞。
  - **逻辑简化**：前端代码移除所有对 `ungrouped` 字符串的条件判断，统一通过真实分组 ID 交互。

---

## [v1.8.0] - 2026-02-22

### 极简风UI全面重构

**工作名称**：极极简风设计原型实现

**设计理念**：
- **空**: 移除所有非必要的装饰、阴影和渐变
- **息**: 增加留白，让视觉有呼吸感，专注于文字本身
- **恒**: 使用经典系统字体栈，保证在任何设备上的一致性
- **信**: 极低饱和度的色彩体系，仅在关键交互处使用色彩

**设计规范 (Design Tokens)**：
- 背景: `#FFFFFF` (纯白) 或 `#FAFAFA` (极浅灰)
- 文字: 主色 `#171717` (近黑)，次要 `#737373` (中灰)
- 品牌色: `#000000` (纯黑) - 用于按钮和强调
- 反馈色: 正确 `#16A34A` (深绿)，错误 `#DC2626` (深红)，待复习 `#D97706` (琥珀色)
- 边框: `1px solid #E5E5E5`
- 圆角: `0px` (硬核极简) 或 `4px` (微圆角)

#### 1. 首页重构 (home.html)

**布局结构**：
- 顶部：左侧文字 Logo "TYPING QUIZ"，右侧 "SIGN OUT"
- 主体居中对齐：
  - 大标题：`TYPING QUIZ` (加粗，大字号)
  - 副标题：`专注于知识内化的打字训练`
  - 核心操作：两个等宽线性按钮 `[ START ]` `[ MANAGE ]`
- 复习看板：置顶展示今日复习统计
- 状态统计：`新测验`、`待复习`、`学习中`、`已暂停`
- 最近活动：列表展示最近练习记录（准确率、用时、时间戳）

#### 2. 测验列表重构 (quizzes.html)

**布局结构**：
- 单列居中窄版布局
- 筛选区：
  - 类型筛选：全部/打字/填空（纯文字链接，无边框）
  - 分组筛选：默认/编程基础/地理常识等（纯文字链接）
- 列表项：
  - 移除卡片感，改为极简列表
  - 每项：标题 (左) | 数量 (右)
  - 悬停：背景 `#F5F5F5`，出现箭头指示
- 模式切换：保留"单个测验"与"分组测验"切换入口

#### 3. 答题界面重构 (index.html)

**布局结构**：
- 顶部：1px 进度条（0-100%）
- 中间：
  - 测验标题：巨大细体文字 (`3rem`)，作为核心视觉焦点
  - 计时器：位于标题下方，灰色加粗 (`1.5rem`)
- 输入框：
  - 位于屏幕正中
  - 仅一条下划线，无边框
  - 字体加大，输入文字居中
- 答案区：
  - 默认显示灰色占位点 `•` 或下划线
  - 答对后文字直接替换占位符
- 复习模式：底部浮现评级面板（重来/困难/良好/简单），包含间隔时间预览

#### 4. 结算界面优化

**布局结构**：
- 内嵌式布局，保留题目区域可见
- 答案项标记：红色边框（未答出）/ 绿色（已答出）
- 统计数据内嵌显示

#### 5. 性能优化

**问题分析**：
- 原测验列表查询：1 + N（测验数）+ M（填空题数）次数据库查询
- 原分组查询：1 + G（分组数）次数据库查询（懒加载 quizzes）

**优化方案**：

| API | 问题 | 优化方案 | 效果 |
|-----|------|---------|------|
| `/api/quizzes` | N+1 查询（填空题详情） | 批量查询答案数量 | 1+N+M → 2 次 |
| `/api/groups` | N+1 查询（懒加载 quizzes） | JOIN FETCH 一次性加载 | 1+G → 1 次 |

**修改文件**：
- `QuizRepository.java`: 新增 `findByUserIdSimple()`、`findAnswerCountsByUserId()`
- `QuizService.java`: 新增 `getAllQuizDTOsForList()` 批量查询方法
- `QuizGroupRepository.java`: 新增 `findByUserIdWithQuizzes()` JOIN FETCH 查询
- `QuizGroupService.java`: 使用优化后的查询方法
- `QuizController.java`: 使用轻量级 DTO 方法

#### 6. 导入功能修复

**问题**：创建页面导入按钮使用旧的模态框方式，与管理页面不一致

**修复**：
- `create.html`: 替换导入功能为文件选择方式，与管理页面一致
- 使用 `/api/import-export/quizzes/import` API

#### 7. 编辑功能修复

**问题**：编辑测验时答案数据未正确加载

**根因**：
- `QuizResponseDTO` 缺少 `answers` 和 `answerList` 字段
- 前端访问字段名不匹配（`quiz.answers` vs `quiz.answerList`，`quiz.content` vs `quiz.fillBlankQuiz.fullText`）

**修复**：
- `QuizResponseDTO.java`: 添加 `answers`、`answerList` 字段
- `QuizService.java`: `toResponseDTO()` 填充答案数据
- `create.html`: 修正字段访问路径

**修改文件**：
- `src/main/resources/static/home.html` - 首页重构
- `src/main/resources/static/quizzes.html` - 测验列表重构
- `src/main/resources/static/index.html` - 答题界面重构
- `src/main/resources/static/style.css` - 全局样式重构
- `src/main/resources/static/prototype.html` - 设计原型参考
- `src/main/resources/static/create.html` - 导入功能修复、编辑功能修复
- `src/main/resources/static/manage.html` - 导入功能优化
- `src/main/java/com/typingquiz/dto/QuizResponseDTO.java` - 添加答案字段
- `src/main/java/com/typingquiz/service/QuizService.java` - 查询优化、DTO 转换优化
- `src/main/java/com/typingquiz/repository/QuizRepository.java` - 新增批量查询
- `src/main/java/com/typingquiz/repository/QuizGroupRepository.java` - JOIN FETCH 优化
- `src/main/java/com/typingquiz/controller/QuizController.java` - 使用优化方法

---

## [v1.7.1] - 2026-02-18

### 问题修复

#### 全局复习测验数量显示错误 ✅

**问题描述**
全局复习模式显示测验总数为 48 个，但用户实际只有 29 个测验（待复习 19 个）。前端控制台显示所有测验的 `userId` 都是 6，误导排查方向。

**根因分析**
1. `QuizReviewStatus` 表中存在用户 6 对其他用户测验（userId 2,3,4,5,7）的复习状态记录（共 40 条异常数据）
2. `ReviewController.getGlobalReviewQuizzes()` 只过滤了 `QuizReviewStatus.userId`，未检查 `Quiz.userId`
3. DTO 中的 `userId` 字段取自 `QuizReviewStatus.userId` 而非 `Quiz.userId`，导致前端显示的 userId 都是当前用户

**排查过程**
1. 前端日志输出所有测验详情，发现 `userId` 全是 6
2. 数据库查询确认：`SELECT quiz_id, quiz_owner_id FROM quiz_review_status JOIN quiz WHERE user_id=6 AND quiz_owner_id!=6` 返回 40 条异常记录
3. 发现 quizId 124-400 的测验属于其他用户（2,3,4,5,7），quizId 491-519 才是用户 6 的测验

**解决方案**
- `ReviewController.java`: 在全局复习查询循环中增加 `quiz.getUserId().equals(userId)` 检查
- 过滤掉测验拥有者与当前用户不匹配的记录

**修改文件**
- `src/main/java/com/typingquiz/controller/ReviewController.java`

**验证结果**
- 全局复习测验数量从 48 降至 29（符合预期）
- 后端日志：`其他用户测验: 40, 无分组: 1, 有效测验: 29`

---

## [v1.7.0] - 2026-02-09

### 新增功能

#### 测验批量管理 

**功能说明**
测验管理页面支持测验的多选操作，可对已选中的测验执行删除、分组等批量操作。

**功能特性**
- **多选复选框**: 每行测验前增加复选框，支持单个选择
- **全选/取消**: 表头全选框一键全选，工具栏按钮取消选择
- **批量删除**: 选中多个测验后一键删除（支持已分组的测验）
- **批量分组**: 选中多个测验后添加到指定分组
- **实时计数**: 工具栏显示已选择的测验数量

**技术实现**
- `manage.html`: 添加复选框列、批量操作工具栏、批量分组模态框
- `manage.js`: 新增 `selectedQuizzes` Set 存储选择状态，实现全选/反选/批量操作逻辑

---

#### 修复填空题加载失败 

**问题描述**
打开填空题测验时显示"加载测验失败"，浏览器控制台报错 `TypeError: this.loadFillBlankQuiz is not a function`。

**根因分析**
`loadFillBlankQuiz()` 方法被 `loadQuizById()` 调用，但方法定义缺失。

**解决方案**
- `quiz-controller.js`: 添加 `loadFillBlankQuiz()` 方法，从 `/api/fill-blank/quiz/{quizId}` 获取填空题数据

---

#### 测验删除自动解绑分组 

**功能说明**
删除已分组的测验时，自动从所有分组中移除关联，无需手动编辑分组。

**修复前问题**
- 测验在分组中时无法直接删除，报外键约束错误
- 必须先到分组编辑页面移除测验，才能删除

**修复后体验**
- 单个删除：点击删除直接成功，自动从分组中移除
- 批量删除：选中的测验（包括已分组的）全部直接删除

**技术实现**
- `QuizService.deleteQuiz()`: 添加代码先查询包含该测验的所有分组，移除关联后再删除测验
- `QuizGroupRepository`: 新增 `findByQuizzesId()` 方法

---

#### 修复导入时分组信息处理问题 

**问题描述**
导入测验时，分组信息无法正确关联，测验被导入到错误的分组或未关联到任何分组。

**根因分析**
1. `findByName()` 未考虑 `userId`，可能匹配到其他用户的同名分组
2. 创建新分组时未设置 `userId`，导致分组归属错误

**解决方案**
- `QuizGroupRepository`: 新增 `findByNameAndUserId()` 方法，按用户隔离查询
- `QuizService.createQuiz()`: 使用用户特定的分组查询，新建分组时正确设置 `userId`

---

## [v1.6.0] - 2026-02-08

### 重大更新

#### 本地开发环境连接云端 MySQL 

**功能说明**
实现本地项目连接云服务器 MySQL，方便开发和测试。

**实现方式**
- 使用环境变量 `MYSQL_HOST` 配置数据库地址
- 本地开发：`MYSQL_HOST=47.102.147.127`
- 云服务器：默认使用 `localhost`

**配置修改**
- `application.properties` 支持 `${MYSQL_HOST:localhost}` 语法
- `start.bat` 自动设置本地环境变量

---

#### 账户数据隔离功能完善 

**功能说明**
完善测验和分组的账户隔离，确保用户只能访问自己的数据。

**实现内容**
- **测验隔离**: 按 userId 过滤查询结果
- **分组隔离**: 添加 `user_id` 字段，按用户查询
- **前端修复**: `manage.js` 添加 Authorization header

**修改文件**
- `QuizController.java` - 添加用户验证
- `QuizService.java` - 按 userId 过滤
- `QuizGroupController.java` - 添加用户验证
- `QuizGroupService.java` - 添加用户过滤
- `QuizGroup.java` - 添加 userId 字段
- `QuizGroupRepository.java` - 添加按用户查询
- `manage.js` - 发送 Authorization header

---

## [v1.5.0] - 2026-02-06

### 重大更新

#### 账户功能与 MySQL 数据库迁移 

**功能说明**
完成用户账户系统功能开发，并将数据库从 H2 切换为 MySQL。

**核心功能**
- **用户注册**: 支持邮箱注册，密码加密存储
- **用户登录**: JWT 认证，Token 有效期 24 小时
- **数据隔离**: 用户只能查看和管理自己的测验数据

**数据库配置**
- 数据库: MySQL 8.0
- 数据库名: `typing_quiz`
- 字符集: `utf8mb4`
- 用户: `typingquiz` (应用连接), `root` (管理)

**技术实现**
- 后端: Spring Security + JWT
- 数据库: MySQL 8.0 + JPA
- 前端: localStorage 存储 Token

---

### 新增功能

#### 示例填空题 ✅

**功能说明**
新用户注册后自动初始化示例测验数据，包含打字题和填空题。

**初始化数据**
- **世界首都**: 52 个首都名称（打字题）
- **古诗词填空**: 静夜思，4 个空（填空题）

---

### 部署说明

#### Docker 网络模式

**原因**: MySQL 安装在主机系统上，不是 Docker 容器

**解决方案**: 使用 `--network host` 模式，让容器共享主机网络

**部署命令**:
```bash
docker run -d --network host --name typing-quiz-app typing-quiz-app
```

**说明**: 这样容器内的 `localhost` 等于主机的 `localhost`，才能正确连接 MySQL

---

### 计划更新

- 待填写

---

## [v1.6.0] - 2026-02-08

### 重大更新

#### 本地开发环境连接云端 MySQL ✅

**功能说明**
实现本地项目连接云服务器 MySQL，方便开发和测试。

**实现方式**
- 使用环境变量 `MYSQL_HOST` 配置数据库地址
- 本地开发：`MYSQL_HOST=47.102.147.127`
- 云服务器：默认使用 `localhost`

**配置修改**
- `application.properties` 支持 `${MYSQL_HOST:localhost}` 语法
- `start.bat` 自动设置本地环境变量

---

#### 账户数据隔离功能完善 ✅

**功能说明**
完善测验和分组的账户隔离，确保用户只能访问自己的数据。

**实现内容**
- **测验隔离**: 按 userId 过滤查询结果
- **分组隔离**: 添加 `user_id` 字段，按用户查询
- **前端修复**: `manage.js` 添加 Authorization header

**修改文件**
- `QuizController.java` - 添加用户验证
- `QuizService.java` - 按 userId 过滤
- `QuizGroupController.java` - 添加用户验证
- `QuizGroupService.java` - 添加用户过滤
- `QuizGroup.java` - 添加 userId 字段
- `QuizGroupRepository.java` - 添加按用户查询
- `manage.js` - 发送 Authorization header

---

## [v1.4.0] - 2026-02-04

### 重大更新

#### 云服务器部署与远程访问 ✅

**功能说明**
成功部署到云服务器，实现外网远程访问，摆脱内网穿透的限制。

**实现方式**
- **服务器**: 阿里云 ECS (47.102.147.127)
- **镜像**: Docker 26.1.3
- **部署方式**: Docker 容器化部署
- **配置文件**: 新增 `Dockerfile` 实现一键构建和运行

**技术实现**
- 编写 `Dockerfile`: 基于 Maven 构建的多阶段镜像
- 容器化运行: `docker run -p 8080:8080 typing-quiz`

---

#### UI/UX 设计优化 ✅

**功能说明**
使用 AI Design Skills (ui-ux-pro-max-skill, frontend-design) 优化页面设计，提升视觉体验和性能。

**优化页面**
- **首页 (home.html)**: 色彩系统升级、背景简化、卡片样式优化
- **测验列表 (quizzes.html)**: 靛蓝主题、卡片设计、按钮样式
- **管理页 (manage.html)**: 数据密集型仪表盘设计

**优化内容**
- **色彩统一**: 采用靛蓝/紫色调 (#6366F1, #4F46E5)
- **性能优化**: 移除 Google Fonts，使用系统字体栈
- **视觉提升**: 阴影、圆角、悬停效果微调

**使用技能**
- `ui-ux-pro-max-skill`: 生成设计系统和建议
- `frontend-design`: 前端设计最佳实践

---

#### 内网穿透测试 ✅

**功能说明**
使用 ngrok 实现临时外网访问，方便测试和演示。

**实现方式**
- 工具: ngrok v3
- 命令: `ngrok http 8080`
- 公网地址: `https://uncatalogued-roger-perfectly.ngrok-free.dev`

**文档**
- 新增 `docs/NGROK.md`: 内网穿透操作指南

---

#### 修复导入测验作答无效问题 ✅

**问题描述**
导入的测验在作答时返回 500 错误，无法正常匹配答案。

**根因分析**
- 导入的答案数据存在重复（同一 quizId + normalized_content 多条）
- `findByQuizIdAndNormalizedContent` 返回多条结果，触发 `NonUniqueResultException`

**解决方案**
- `AnswerRepository.java`: 改为 `findFirstByQuizIdAndNormalizedContent` 取第一个匹配项
- `AnswerController.java`: 添加异常处理和日志记录

**涉及文件**
- `src/main/java/com/typingquiz/repository/AnswerRepository.java`
- `src/main/java/com/typingquiz/controller/AnswerController.java`
- `src/main/java/com/typingquiz/service/AnswerService.java`

---

## [v1.3.0] - 2026-01-31

### 新增功能

#### 测验管理增强 ✅

**功能说明**
增强测验管理界面的信息展示和筛选功能。

**功能特性**
- **类型列**: 显示测验类型（打字题/填空题）
- **分组列**: 显示测验所属分组，不在分组中显示 `-`
- **类型筛选器**: 可按类型筛选全部/打字题/填空题

**技术实现**
- `manage.html`: 添加类型列、分组列和筛选器下拉菜单
- `manage.js`: 新增 `renderQuizTable()` 和 `filterQuizzes()` 函数
- 构建测验到分组的映射：`quizGroupMap[quizId] = groupName`

---

#### 测验列表筛选器 ✅

**功能说明**
测验列表页新增分组/类型筛选器，支持组合筛选单个测验。

**功能特性**
- 分组下拉筛选（全部/指定分组）
- 类型筛选（全部/打字题/填空题）
- 分组与类型组合过滤

**技术实现**
- `quizzes.html`: 新增筛选条与筛选逻辑
- 统一分组 `quizIds` 类型用于筛选

---

#### 填空题显示与换行优化 ✅

**功能说明**
填空题答案/注释按行尾拆分为单行框，未作答时占位框按答案长度拆分，避免超出容器。

**功能特性**
- 占位框按行尾拆分显示，单行框高度一致
- 注释颜色更浅，与答案区分
- 文本基线对齐，视觉更统一
- 未作答时灰色占位框支持多行显示

**技术实现**
- `quiz-controller.js`: 基于真实容器测量宽度并拆分渲染
- `style.css`: 占位框对齐/颜色/字体样式调整

---

## [v1.2.0] - 2026-01-31

### 新增功能

#### 注释语法统一 ✅

**功能说明**
统一填空题和打字题的注释语法，简化使用体验。

**功能特性**
- **统一语法**: 所有题型使用 `##注释##` 格式
- **填空题**: `[答案##注释##]`
- **打字题**: `答案##注释##`
- **注释按钮**: 选中文字后点击按钮自动添加 `##` 标记

**技术实现**
- `manage.html`: 添加"📝 注释选中文字"按钮
- `manage.js`: 新增 `wrapSelectionWithComment()` 和 `wrapAnswerWithComment()` 函数
- 正则表达式解析: `/^(.+?)##(.+?)##$/`

---

#### 填空题注释显示优化 ✅

**功能说明**
优化填空题注释的显示时机和框宽度计算。

**功能特性**
- **显示时机**: 注释仅在作答后显示
- **宽度自适应**: 框宽度根据答案+注释长度动态调整
- **放弃后显示**: 放弃后显示正确答案和注释

**技术实现**
- `quiz-controller.js`: 更新 `renderFillBlankQuiz()` 和 `renderFillBlankQuizForGiveUp()`
- 宽度计算: `Math.max((answer + comment).length + 2, 4) + 'em'`

---

## [v1.1.0] - 2026-01-30

### 新增功能

#### 答案注释功能 ✅

**功能说明**
用户可以在创建测验时为答案添加注释，注释部分不参与答题验证，仅在显示答案时以灰色文字展示。

**使用方法**

**打字题注释**
1. 创建打字题时，在答案后使用 `#` 分隔答案和注释
2. 格式：`答案#注释内容`
3. 示例：`北京#中国首都`
4. 答题时只需输入 `北京`，答对后显示 `北京#中国首都`

**填空题注释**
1. 创建填空题时，在挖空内容内使用 `#` 分隔答案和注释
2. 格式：`[答案#注释内容]`
3. 示例：`我爱吃[苹果#一种红色的水果]和香蕉`
4. 答题时只需输入 `苹果`

**显示效果**
- 注释以灰色斜体文字显示在答案后面
- 在答案网格、高亮显示、放弃后的结果中都会显示注释
- 注释不影响答题验证逻辑

**技术实现**
- 后端: 新增 `AnswerCreateDTO`，更新 `Answer` 实体添加 `comment` 字段
- 前端: 解析注释逻辑，UI 渲染支持注释显示
- 数据库: `answer` 表新增 `comment` 字段

**问题修复**
- 修复填空题放弃功能无法显示结果面板的问题
- 修复填空题放弃后未显示正确答案的问题

---

#### 测验分组功能 ✅

**功能说明**
用户可以自由创建测验分组，将多个测验归类到同一分组下，便于管理和组织。

**使用方法**
1. 访问测验管理页面 (http://localhost:8080/manage.html)
2. 在"测验分组"区域点击"创建新分组"
3. 输入分组名称和描述
4. 选择要包含的测验（可多选）
5. 点击保存

**编辑分组**
1. 点击分组列表中的"编辑"按钮
2. 修改分组信息或调整关联的测验
3. 点击保存

**删除分组**
1. 点击分组列表中的"删除"按钮
2. 确认删除（测验不会被删除）

**API端点**
- `GET /api/groups` - 获取所有分组
- `POST /api/groups` - 创建分组
- `PUT /api/groups/{id}` - 更新分组
- `DELETE /api/groups/{id}` - 删除分组
- `POST /api/groups/{groupId}/quizzes/{quizId}` - 添加测验到分组
- `DELETE /api/groups/{groupId}/quizzes/{quizId}` - 从分组移除测验

**技术实现**
- 后端: 新增 `QuizGroup` 实体、`QuizGroupRepository`、`QuizGroupService`、`QuizGroupController`
- 数据库: 使用多对多关联表 `quiz_group_quiz`
- 前端: 新增分组管理 UI 和模态框

---

#### 分组答题模式 ✅

**功能说明**
支持按分组进行连续答题，同一分组内的测验可以使用左右箭头键切换，方便用户连续完成多个相关测验。

**使用方法**
1. 访问测验列表页面 (http://localhost:8080/quizzes.html)
2. 点击"分组测验"切换按钮
3. 选择一个分组开始答题
4. 答题过程中使用 `←` `→` 键切换到上一个/下一个测验
5. 完成所有测验后显示分组汇总结果

**功能特性**
- **模式切换**: 支持"单个测验"和"分组测验"两种模式
- **进度显示**: 实时显示当前测验在分组中的位置
- **键盘导航**: 使用左右箭头键在测验间切换
- **结果汇总**: 分组完成后显示各测验得分汇总

**技术实现**
- 后端: 新增 `GET /api/groups/{groupId}/quizzes` API
- 前端:
  - `quizzes.html`: 新增分组模式切换和分组列表展示
  - `quiz-controller.js`: 新增分组模式支持、箭头导航、结果汇总
  - `style.css`: 新增分组模式样式

---

### 问题修复

#### 填空题导出功能修复 ✅

**问题描述**
导出填空题时，`fillBlankQuiz` 字段为空JSON对象，无法正常保存和导入填空题数据。

**修复内容**
- 更新 `ImportExportController.convertToDTO()` 方法
- 添加 `FillBlankQuizRepository` 依赖注入
- 导出时查询并包含完整的填空题信息（fullText、displayText、blanks等）
- 解析 `blanksInfo` JSON字段为结构化数据

**导出格式示例**
```json
{
  "title": "水果测验",
  "quizType": "FILL_BLANK",
  "fillBlankQuiz": {
    "fullText": "我爱吃苹果和香蕉",
    "displayText": "我爱吃___和___",
    "blanksCount": 2,
    "blanks": [
      {"startIndex": 4, "endIndex": 6, "correctAnswer": "苹果"},
      {"startIndex": 9, "endIndex": 11, "correctAnswer": "香蕉"}
    ]
  }
}
```

---

#### 分组答题进度保存与恢复 ✅

**功能说明**
在分组答题模式下，支持保存和恢复每个测验的答题进度。切换回已完成的测验时，可以正确恢复之前的答题状态。

**功能特性**
- **进度保存**: 切换测验时自动保存当前测验的答题进度
- **进度恢复**: 切换回已完成的测验时，自动恢复之前的答题状态
- **放弃状态保留**: 放弃的测验切换后再切回时，保持放弃状态并显示正确答案
- **答案恢复**: 已答出的答案会保持绿色高亮状态

**技术实现**
- 新增 `groupProgress` Map 存储各测验的进度
- `saveCurrentQuizProgress()`: 保存打字题和填空题的答题进度
- `saveGiveUpProgress()`: 单独保存放弃状态的进度
- `restoreQuizProgress()`: 恢复测验进度和完成状态
- `isQuizCompleted()`: 检查测验是否已完成
- `showCompletedQuizResult()`: 正确渲染已完成测验的结果界面

**问题修复**
- 修复切换测验后输入框和放弃按钮缺失的问题
- 修复放弃后切换再切回时显示"恭喜!全部答对!"的错误文案
- 修复放弃后切换再切回时答案网格不显示的问题
- 修复放弃状态被后续操作覆盖的问题

---

#### 填空题显示优化 ✅

**功能说明**
优化填空题在答题时的显示效果，包括换行、占位符大小和放弃后的颜色区分。

**功能特性**
- **换行显示**: 手动换行（Enter键）的行距大于自动换行
- **占位符宽度**: 根据答案长度动态调整框的宽度
- **占位符高度**: 所有状态（未答题、已答题、放弃）的框高度一致
- **文字居中**: 框内文字居中显示，左右间距统一
- **放弃颜色区分**: 放弃后，已答题显示绿色框，未答题显示红色框

**技术实现**
- CSS添加 `white-space: pre-wrap` 支持换行符显示
- 使用 `<div class="manual-break">` 替代 `<br>` 实现更大的手动换行间距
- JavaScript动态计算框宽度：`Math.max(answerLength + 2, 4) + 'em'`
- 使用 `inline-flex` 和 `align-items: baseline` 实现文字对齐
- `renderFillBlankQuizForGiveUp()`: 放弃时渲染，区分已答题和未答题的颜色

**问题修复**
- 修复填空题换行显示挤在一起的问题
- 修复占位符框大小不随答案长度变化的问题
- 修复绿框和灰框高度不一致的问题
- 修复放弃后所有框都显示绿色的问题

---

#### 填空题创建优化 ✅

**功能说明**
优化填空题的创建方式，提供更便捷的挖空功能。

**功能特性**
- **挖空按钮**: 选中文字后点击按钮自动添加 `[]`
- **预览功能**: 实时预览挖空后的文本效果

**技术实现**
- `manage.html`: 添加"🔍 挖空选中文字"按钮
- `manage.js`: 新增 `wrapSelectionWithBrackets()` 函数
- 使用 `selectionStart` 和 `selectionEnd` 获取选中文本位置

---

## [v1.0.0] - 2025-01-10

### 基础功能

#### 打字测验核心功能 ✅

**功能说明**
JetPunk风格的打字测验Web应用，支持多种测验模式。

**主要功能**
- 打字测验：输入正确答案
- 填空测验：从句子中填写缺失的词
- 即时答案匹配
- 计时器功能（倒计时/正计时）
- 得分统计
- 结果展示

---

#### 数据管理增强 - 导入导出功能 ✅

**功能说明**
支持将测验数据导出为JSON格式，也可以从JSON文件导入测验数据，方便数据备份和迁移。

**使用方法**

**导出单个测验:**
1. 访问测验管理页面 (http://localhost:8080/manage.html)
2. 在测验列表中找到要导出的测验
3. 点击"导出"按钮
4. 浏览器会自动下载JSON文件

**导出所有测验:**
1. 访问测验管理页面
2. 点击页面顶部的"导出所有测验"按钮
3. 浏览器会下载包含所有测验的JSON文件

**导入测验:**
1. 访问测验管理页面
2. 点击页面顶部的"导入测验"按钮
3. 选择JSON文件（可以是单个测验或多个测验）
4. 系统会显示导入结果（成功/失败数量）

**JSON格式示例**

单个测验:
```json
{
  "title": "世界首都",
  "description": "说出世界各国的首都",
  "timeLimit": 600,
  "answers": ["北京", "东京", "伦敦", "巴黎", "华盛顿"]
}
```

多个测验:
```json
[
  {
    "title": "测验1",
    "description": "描述1",
    "timeLimit": 300,
    "answers": ["答案1", "答案2"]
  },
  {
    "title": "测验2",
    "description": "描述2",
    "timeLimit": null,
    "answers": ["答案3", "答案4"]
  }
]
```

**API端点**
- `GET /api/import-export/quiz/{id}/export` - 导出单个测验
- `GET /api/import-export/quizzes/export` - 导出所有测验
- `POST /api/import-export/quiz/import` - 导入单个测验
- `POST /api/import-export/quizzes/import` - 批量导入测验

---

#### 测验放弃功能 ✅

**功能说明**
在测验过程中，如果觉得太难或想查看答案，可以点击"放弃"按钮。放弃后：
- 所有未答出的答案会在原位置以**红色**显示
- 已答出的答案保持绿色显示
- 显示最终结果统计

**使用方法**
1. 开始任意测验
2. 在答题过程中，点击输入框旁边的红色"放弃"按钮
3. 确认放弃
4. 查看所有答案（未答出的用红色标注）

**界面变化**
- 输入框旁边新增红色"放弃"按钮
- 放弃后，答案网格中：
  - ✅ 绿色 = 已答出的答案
  - ❌ 红色 = 未答出的答案
- 结果统计中会显示"（已放弃）"标记

---

#### 数据库管理实时搜索 ✅

**功能说明**
数据库管理页面现在支持实时搜索，无需点击搜索按钮：
- 输入时自动搜索（300ms防抖）
- 显示所有测验/答案的初始列表
- 搜索结果随输入实时更新
- 清空输入框自动恢复完整列表

**使用方法**

**按测验检索:**
1. 访问数据库管理页面 (http://localhost:8080/database.html)
2. 在"按测验检索"标签页
3. 页面加载时自动显示所有测验列表
4. 在搜索框中输入：
   - 纯数字 → 按测验ID搜索
   - 文字 → 按测验名称模糊搜索
5. 搜索结果实时更新
6. 清空输入框恢复完整列表

**按答案检索:**
1. 切换到"按答案检索"标签页
2. 页面加载时自动显示所有答案列表
3. 在搜索框中输入：
   - 纯数字 → 按答案ID搜索
   - 文字 → 按答案内容模糊搜索
4. 搜索结果实时更新
5. 清空输入框恢复完整列表

**改进点**
- ❌ 移除了"搜索"按钮
- ✅ 支持实时搜索（输入即搜索）
- ✅ 初始显示完整列表
- ✅ 300ms防抖优化性能
- ✅ 空输入自动恢复列表

**技术实现**
- 后端: Spring Boot 2.7.18, Spring Data JPA, H2 Database
- 前端: HTML5, CSS3, Vanilla JavaScript
- 数据持久化: H2文件数据库

---

## 📊 功能对比

| 功能 | v1.0.0 | v1.1.0 |
|------|--------|--------|
| 打字测验 | ✅ | ✅ |
| 填空测验 | ✅ | ✅ |
| 数据持久化 | ✅ | ✅ |
| 测验管理 CRUD | ✅ | ✅ |
| 数据导出 | ✅ | ✅ |
| 数据导入 | ✅ | ✅ |
| 测验放弃 | ✅ | ✅ |
| 数据库搜索 | ✅ 实时搜索 | ✅ 实时搜索 |
| 初始列表 | ✅ | ✅ |
| 测验分组 | ❌ | ✅ |
| 答案注释 | ❌ | ✅ |

---

## 📝 注意事项

1. **导入数据格式**: 确保JSON格式正确，必须包含 `title` 和 `answers` 字段
2. **放弃操作**: 放弃后无法继续答题，只能重新开始
3. **实时搜索**: 搜索有300ms延迟，避免输入时频繁请求
4. **数据备份**: 建议定期导出数据作为备份
5. **分组操作**: 删除分组不会删除关联的测验

---

**最后更新:** 2026-01-30
