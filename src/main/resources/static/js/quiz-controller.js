/**
 * QuizController - 测验主控制器
 * 负责管理测验的整个生命周期
 */
class QuizController {
    constructor(quizId) {
        this.quizId = quizId;
        this.quiz = null;
        this.answers = [];
        this.foundAnswers = new Set();
        this.timer = null;
        this.isQuizActive = false;
        this.apiBase = '/api';
        this.quizType = 'TYPING'; // TYPING or FILL_BLANK
        this.fillBlankQuiz = null;
        this.filledBlanks = new Map(); // blankIndex -> userAnswer

        this.groupMode = false;
        this.groupId = null;
        this.isReviewMode = false;
        this.groupQuizzes = []; // 分组中的所有测验
        this.currentQuizIndex = 0; // 当前测验索引
        this.groupProgress = new Map(); // 存储各测验的进度 {quizId -> {foundAnswers, filledBlanks, completed}}
        this.groupScores = new Map();
    }

    getAuthHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };
        try {
            let token = null;
            if (typeof Auth !== 'undefined' && Auth.getToken) {
                token = Auth.getToken();
            }
            if (!token && typeof localStorage !== 'undefined') {
                token = localStorage.getItem('typingquiz_token');
            }
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        } catch (e) {
            // ignore
        }
        return headers;
    }

    ensureFillBlankMeasureEl(textEl) {
        if (!this._fillBlankMeasureEl) {
            const el = document.createElement('span');
            el.className = 'fill-blank-placeholder';
            el.style.position = 'absolute';
            el.style.visibility = 'hidden';
            el.style.left = '-99999px';
            el.style.top = '-99999px';
            el.style.pointerEvents = 'none';
            el.innerHTML = '<span class="fill-blank-main"></span><span class="fill-blank-comment"></span>';
            this._fillBlankMeasureEl = el;
            this._fillBlankMeasureMainEl = el.querySelector('.fill-blank-main');
            this._fillBlankMeasureCommentEl = el.querySelector('.fill-blank-comment');
        }

        if (textEl && this._fillBlankMeasureEl.parentElement !== textEl) {
            textEl.appendChild(this._fillBlankMeasureEl);
        }

        const style = window.getComputedStyle(this._fillBlankMeasureEl);
        this._fillBlankMeasureMarginLeft = parseFloat(style.marginLeft || '0');
        this._fillBlankMeasureMarginRight = parseFloat(style.marginRight || '0');
        const commentStyle = window.getComputedStyle(this._fillBlankMeasureCommentEl);
        this._fillBlankMeasureCommentMarginLeft = parseFloat(commentStyle.marginLeft || '0');
    }

    measureFillBlankBoxWidthPx(textEl, mainText, commentText, includeHash = false) {
        this.ensureFillBlankMeasureEl(textEl);
        const main = mainText || '';
        const comment = commentText ? (includeHash ? ('#' + commentText) : commentText) : '';
        this._fillBlankMeasureMainEl.textContent = main;
        if (comment) {
            this._fillBlankMeasureCommentEl.textContent = comment;
            this._fillBlankMeasureCommentEl.style.display = '';
        } else {
            this._fillBlankMeasureCommentEl.textContent = '';
            this._fillBlankMeasureCommentEl.style.display = 'none';
        }
        let width = this._fillBlankMeasureEl.getBoundingClientRect().width;
        if (comment) {
            width += this._fillBlankMeasureCommentMarginLeft || 0;
        }
        return Math.ceil(width + 2);
    }

    measureFillBlankBoxOuterWidthPx(textEl, mainText, commentText, includeHash = false) {
        const innerWidth = this.measureFillBlankBoxWidthPx(textEl, mainText, commentText, includeHash);
        return innerWidth + this._fillBlankMeasureMarginLeft + this._fillBlankMeasureMarginRight;
    }

    replaceFillBlankWrappers(textEl) {
        if (!textEl) return;
        const wrappers = Array.from(textEl.querySelectorAll('.fill-blank-wrapper'));
        if (wrappers.length === 0) return;

        const textRect = textEl.getBoundingClientRect();
        const style = window.getComputedStyle(textEl);
        const paddingRight = parseFloat(style.paddingRight || '0');
        const paddingLeft = parseFloat(style.paddingLeft || '0');
        const containerLeft = textRect.left + paddingLeft;
        const containerRight = textRect.right - paddingRight;
        const fullLineWidth = Math.max(0, containerRight - containerLeft);

        wrappers.forEach(wrapper => {
            const blankIndex = parseInt(wrapper.getAttribute('data-blank-index') || '0', 10);
            const isFilled = wrapper.getAttribute('data-filled') === '1';
            const mode = wrapper.getAttribute('data-mode') || 'play';
            const state = wrapper.getAttribute('data-state') || '';

            const answerText = decodeURIComponent(wrapper.getAttribute('data-answer') || '');
            const correctText = decodeURIComponent(wrapper.getAttribute('data-correct') || '');
            const comment = decodeURIComponent(wrapper.getAttribute('data-comment') || '');

            const isEmptyPlay = mode === 'play' && !isFilled;
            const baseText = (isEmptyPlay ? correctText : (isFilled ? answerText : correctText)) || '';
            const commentToShow = (mode === 'giveup') ? comment : (isFilled ? comment : '');

            const wrapperRect = wrapper.getBoundingClientRect();
            const availableFirst = Math.max(0, containerRight - wrapperRect.left);
            const minOuterWidth = this.measureFillBlankBoxOuterWidthPx(textEl, '一', '');

            const frag = document.createDocumentFragment();
            let mainIndex = 0;
            let commentIndex = 0;
            let isFirst = true;
            let isFirstSegment = true;

            while (mainIndex < baseText.length || commentIndex < commentToShow.length) {
                const avail = isFirst
                    ? (availableFirst >= minOuterWidth ? availableFirst : fullLineWidth)
                    : fullLineWidth;

                let mainChunk = '';
                let commentChunk = '';
                let includeHash = false;

                if (mainIndex < baseText.length) {
                    let low = 1;
                    let high = baseText.length - mainIndex;
                    let best = 1;
                    while (low <= high) {
                        const mid = Math.floor((low + high) / 2);
                        const part = baseText.slice(mainIndex, mainIndex + mid);
                        const width = this.measureFillBlankBoxOuterWidthPx(textEl, part, '', false);
                        if (width <= avail || avail === 0) {
                            best = mid;
                            low = mid + 1;
                        } else {
                            high = mid - 1;
                        }
                    }

                    mainChunk = baseText.slice(mainIndex, mainIndex + best);
                    mainIndex += best;

                    if (mainIndex >= baseText.length && commentToShow.length > 0) {
                        includeHash = commentIndex === 0;
                        let lowC = 1;
                        let highC = commentToShow.length - commentIndex;
                        let bestC = 0;
                        while (lowC <= highC) {
                            const midC = Math.floor((lowC + highC) / 2);
                            const partC = commentToShow.slice(commentIndex, commentIndex + midC);
                            const width = this.measureFillBlankBoxOuterWidthPx(textEl, mainChunk, partC, includeHash);
                            if (width <= avail || avail === 0) {
                                bestC = midC;
                                lowC = midC + 1;
                            } else {
                                highC = midC - 1;
                            }
                        }

                        if (bestC > 0) {
                            commentChunk = commentToShow.slice(commentIndex, commentIndex + bestC);
                            commentIndex += bestC;
                        }
                    }
                } else {
                    includeHash = commentIndex === 0;
                    let lowC = 1;
                    let highC = commentToShow.length - commentIndex;
                    let bestC = 1;
                    while (lowC <= highC) {
                        const midC = Math.floor((lowC + highC) / 2);
                        const partC = commentToShow.slice(commentIndex, commentIndex + midC);
                        const width = this.measureFillBlankBoxOuterWidthPx(textEl, '', partC, includeHash);
                        if (width <= avail || avail === 0) {
                            bestC = midC;
                            lowC = midC + 1;
                        } else {
                            highC = midC - 1;
                        }
                    }

                    commentChunk = commentToShow.slice(commentIndex, commentIndex + bestC);
                    commentIndex += bestC;
                }

                const span = document.createElement('span');
                span.className = 'fill-blank-placeholder';
                if (state) {
                    span.classList.add(state);
                } else if (mode === 'play' && isFilled) {
                    span.classList.add('filled');
                }

                span.setAttribute('data-blank-index', String(blankIndex));
                span.onclick = () => this.focusFillBlankInput();

                const widthPx = this.measureFillBlankBoxWidthPx(textEl, mainChunk, commentChunk, includeHash);
                span.style.width = widthPx + 'px';
                span.textContent = isEmptyPlay ? '' : mainChunk;

                if (commentChunk) {
                    const c = document.createElement('span');
                    c.className = 'fill-blank-comment';
                    c.textContent = (includeHash ? '#' : '') + commentChunk;
                    span.appendChild(c);
                }

                const underline = document.createElement('span');
                underline.className = 'fill-blank-underline';
                underline.style.position = 'absolute';
                underline.style.bottom = '3px';
                underline.style.left = '4px';
                underline.style.right = '4px';
                underline.style.borderBottom = `2px solid rgba(255,255,255,${(mode === 'play' && isFilled) || mode === 'giveup' ? '0.3' : '0.5'})`;
                span.appendChild(underline);

                if (!isFirstSegment) {
                    const br = document.createElement('br');
                    br.className = 'fill-blank-segment-break';
                    frag.appendChild(br);
                }

                frag.appendChild(span);
                isFirst = false;
                isFirstSegment = false;
            }

            wrapper.replaceWith(frag);
        });
    }

    /**
     * 初始化测验
     */
    async init() {
        try {
            console.log('[QuizController.init] 开始初始化，quizId:', this.quizId);
            
            // 检查是否是分组模式
            this.checkGroupMode();
            console.log('[QuizController.init] groupMode:', this.groupMode, 'groupId:', this.groupId, 'isReviewMode:', this.isReviewMode);

            if (this.groupMode) {
                // 分组模式：从 sessionStorage 获取分组测验列表
                console.log('[QuizController.init] 分组模式，groupId:', this.groupId);
                
                const storedList = sessionStorage.getItem(`groupQuizList_${this.groupId}`);
                let quizList = [];
                
                if (storedList) {
                    try {
                        quizList = JSON.parse(storedList);
                        console.log('[QuizController.init] 从 sessionStorage 获取分组列表:', quizList.length, '个测验');
                    } catch (e) {
                        console.error('[QuizController.init] 解析 sessionStorage 失败:', e);
                    }
                }
                
                // 如果 sessionStorage 没有列表，需要重新获取
                if (quizList.length === 0) {
                    console.log('[QuizController.init] sessionStorage 为空，重新获取分组列表');
                    await this.loadGroupQuizzes();
                    quizList = this.groupQuizzes.map(q => q.id);
                    sessionStorage.setItem(`groupQuizList_${this.groupId}`, JSON.stringify(quizList));
                } else {
                    // 加载测验数据
                    await this.loadGroupQuizzes();
                }
                
                // 计算进度
                const urlQuizId = this.quizId;
                const currentIndex = quizList.indexOf(urlQuizId);
                
                // 保存原始列表长度用于显示
                this.groupQuizTotal = quizList.length;
                
                if (currentIndex >= 0) {
                    // 当前测验在列表中
                    this.currentQuizIndex = currentIndex;
                    console.log('[QuizController.init] 分组进度:', currentIndex + 1, '/', quizList.length);
                    await this.loadQuizById(urlQuizId);
                } else if (this.groupQuizzes.length > 0) {
                    // 当前测验不在列表中（已完成），加载第一个未完成的
                    this.currentQuizIndex = quizList.length - this.groupQuizzes.length;
                    console.log('[QuizController.init] 测验已完成，分组进度:', this.currentQuizIndex + 1, '/', quizList.length);
                    await this.loadQuizById(this.groupQuizzes[0].id);
                } else {
                    // 列表为空，全部完成
                    console.log('[QuizController.init] 分组全部完成');
                    sessionStorage.removeItem(`groupQuizList_${this.groupId}`);
                    alert('该分组复习任务已全部完成！');
                    window.location.href = 'review.html';
                }
            } else if (this.isReviewMode) {
                // 全局复习模式：从 sessionStorage 获取测验列表
                console.log('[QuizController.init] 全局复习模式');
                
                const storedList = sessionStorage.getItem('reviewQuizList');
                let quizList = [];
                
                if (storedList) {
                    try {
                        quizList = JSON.parse(storedList);
                        console.log('[QuizController.init] 从 sessionStorage 获取列表:', quizList.length, '个测验');
                    } catch (e) {
                        console.error('[QuizController.init] 解析 sessionStorage 失败:', e);
                    }
                }
                
                // 如果 sessionStorage 没有列表，需要重新获取
                if (quizList.length === 0) {
                    console.log('[QuizController.init] sessionStorage 为空，重新获取列表');
                    await this.loadGlobalReviewQuizzes();
                    quizList = this.groupQuizzes.map(q => q.id);
                    sessionStorage.setItem('reviewQuizList', JSON.stringify(quizList));
                } else {
                    // 加载测验数据
                    await this.loadGlobalReviewQuizzes();
                }
                
                // 计算进度
                const urlQuizId = this.quizId;
                const currentIndex = quizList.indexOf(urlQuizId);
                
                // 保存原始列表长度用于显示
                this.reviewQuizTotal = quizList.length;
                
                if (currentIndex >= 0) {
                    // 当前测验在列表中
                    this.currentQuizIndex = currentIndex;
                    console.log('[QuizController.init] 进度:', currentIndex + 1, '/', quizList.length);
                    await this.loadQuizById(urlQuizId);
                } else if (this.groupQuizzes.length > 0) {
                    // 当前测验不在列表中（已完成），加载第一个未完成的
                    this.currentQuizIndex = quizList.length - this.groupQuizzes.length;
                    console.log('[QuizController.init] 测验已完成，进度:', this.currentQuizIndex + 1, '/', quizList.length);
                    await this.loadQuizById(this.groupQuizzes[0].id);
                } else {
                    // 列表为空，全部完成
                    console.log('[QuizController.init] 全部完成');
                    sessionStorage.removeItem('reviewQuizList');
                    alert('今日复习任务已全部完成！');
                    window.location.href = 'review.html';
                }
            } else {
                console.log('[QuizController.init] 非分组模式，加载测验:', this.quizId);
                await this.loadQuizById(this.quizId);
            }

            this.setupEventListeners();
            this.startTimer();
            this.isQuizActive = true;

            // 聚焦输入框
            document.getElementById('answer-input').focus();
            console.log('[QuizController.init] 初始化完成');
        } catch (error) {
            console.error('[QuizController.init] 初始化失败:', error);
            alert('加载测验失败,请刷新页面重试');
        }
    }

    /**
     * 检查是否是分组模式
     */
    checkGroupMode() {
        const params = new URLSearchParams(window.location.search);
        this.groupId = params.get('groupId');
        this.groupMode = !!this.groupId;
        this.isReviewMode = params.get('mode') === 'review';
    }

    /**
     * 加载分组测验列表
     * 复习模式下加载待复习列表，普通模式下加载所有测验
     */
    async loadGroupQuizzes() {
        console.log('[loadGroupQuizzes] isReviewMode:', this.isReviewMode, 'groupId:', this.groupId);
        
        if (this.isReviewMode) {
            // 复习模式：加载待复习的测验列表
            const url = `${this.apiBase}/review/groups/${this.groupId}/quizzes`;
            console.log('[loadGroupQuizzes] 复习模式，请求URL:', url);
            
            const response = await fetch(url, {
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                throw new Error('加载分组复习列表失败');
            }
            const reviewItems = await response.json();
            console.log('[loadGroupQuizzes] 复习模式，API返回', reviewItems.length, '个测验');
            
            // 过滤：只保留待复习的测验（label为PENDING_LEARN或PENDING_REVIEW）
            const dueItems = reviewItems.filter(item => item.label === 'PENDING_LEARN' || item.label === 'PENDING_REVIEW');
            console.log('[loadGroupQuizzes] 过滤后，待复习测验:', dueItems.length);
            console.log('[loadGroupQuizzes] 按标签统计:', {
                待学习: dueItems.filter(i => i.label === 'PENDING_LEARN').length,
                待复习: dueItems.filter(i => i.label === 'PENDING_REVIEW').length
            });
            
            // 转换为 quizzes 格式
            this.groupQuizzes = dueItems.map(item => ({
                id: item.quizId,
                title: item.quizTitle,
                status: item.status,
                label: item.label
            }));
        } else {
            // 普通模式：加载分组所有测验
            const url = `${this.apiBase}/groups/${this.groupId}/quizzes`;
            console.log('[loadGroupQuizzes] 普通模式，请求URL:', url);
            
            const response = await fetch(url, {
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                throw new Error('加载分组测验失败');
            }
            this.groupQuizzes = await response.json();
            console.log('[loadGroupQuizzes] 普通模式，获取到', this.groupQuizzes.length, '个测验');
        }

        console.log('[loadGroupQuizzes] 最终 groupQuizzes.length:', this.groupQuizzes.length);

        if (this.groupQuizzes.length === 0) {
            throw new Error('该分组中没有测验');
        }
    }

    /**
     * 加载全局待复习测验列表
     */
    async loadGlobalReviewQuizzes() {
        const url = `${this.apiBase}/review/quizzes`;
        console.log('[loadGlobalReviewQuizzes] 请求URL:', url);
        
        const response = await fetch(url, {
            headers: this.getAuthHeaders()
        });
        if (!response.ok) {
            throw new Error('加载今日复习列表失败');
        }
        const reviewItems = await response.json();
        console.log('[loadGlobalReviewQuizzes] API返回', reviewItems.length, '个测验');
        
        // 打印第一个LEARNING测验的详情
        const firstLearning = reviewItems.find(item => item.label === 'PENDING_LEARN' || item.label === 'PENDING_REVIEW');
        if (firstLearning) {
            console.log('[loadGlobalReviewQuizzes] 第一个待处理测验详情:', {
                quizId: firstLearning.quizId,
                title: firstLearning.quizTitle,
                status: firstLearning.status,
                label: firstLearning.label,
                labelDisplay: firstLearning.labelDisplay
            });
        }
        
        // 过滤：只保留待复习的测验（label 为 PENDING_LEARN 或 PENDING_REVIEW）
        const dueItems = reviewItems.filter(item => {
            const isDue = item.label === 'PENDING_LEARN' || item.label === 'PENDING_REVIEW';
            return isDue;
        });
        console.log('[loadGlobalReviewQuizzes] 过滤后，待复习测验:', dueItems.length);
        console.log('[loadGlobalReviewQuizzes] 按标签统计:', {
            待学习: dueItems.filter(i => i.label === 'PENDING_LEARN').length,
            待复习: dueItems.filter(i => i.label === 'PENDING_REVIEW').length
        });
        
        // 详细统计：按状态和分组
        const groupStats = {};
        dueItems.forEach(item => {
            const key = `${item.status}`;
            if (!groupStats[key]) groupStats[key] = [];
            groupStats[key].push({
                quizId: item.quizId,
                title: item.quizTitle,
                label: item.label
            });
        });
        console.log('[loadGlobalReviewQuizzes] 详细列表:', groupStats);
        
        // 输出全部待复习测验
        console.log('[loadGlobalReviewQuizzes] 全部待复习测验列表:');
        dueItems.forEach((item, index) => {
            console.log(`  ${index + 1}. quizId=${item.quizId}, title="${item.quizTitle}", userId=${item.userId}, status=${item.status}, label=${item.label}`);
        });
        
        // 转换为 quizzes 格式
        this.groupQuizzes = dueItems.map(item => ({
            id: item.quizId,
            title: item.quizTitle,
            status: item.status,
            due: item.due
        }));

        if (this.groupQuizzes.length === 0) {
            throw new Error('今日没有待复习测验');
        }
    }

    /**
     * 重置测验UI状态
     */
    resetQuizUI() {
        this.foundAnswers.clear();
        this.filledBlanks.clear();
        
        // 重置输入框
        const input = document.getElementById('answer-input');
        if (input) {
            input.value = '';
            input.disabled = false;
        }
        
        // 重置放弃按钮
        const giveUpBtn = document.getElementById('give-up-btn');
        if (giveUpBtn) {
            giveUpBtn.disabled = false;
        }
        
        // 清除反馈
        UIRenderer.showFeedback('', '');
    }

    /**
     * 加载填空题数据
     */
    async loadFillBlankQuiz() {
        try {
            const response = await fetch(`${this.apiBase}/fill-blank/quiz/${this.quizId}`, {
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                throw new Error('加载填空题数据失败');
            }
            this.fillBlankQuiz = await response.json();
        } catch (error) {
            console.error('加载填空题失败:', error);
            throw error;
        }
    }

    /**
     * 根据ID加载测验
     */
    async loadQuizById(quizId) {
        // 停止之前的计时器
        if (this.timer) {
            this.timer.stop();
            this.timer = null;
        }

        this.quizId = quizId;
        this.quiz = null;
        this.answers = [];
        this.foundAnswers = new Set();
        this.fillBlankQuiz = null;
        this.filledBlanks = new Map();

        const quizResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}`, {
            headers: this.getAuthHeaders()
        });
        if (!quizResponse.ok) {
            throw new Error('测验不存在');
        }
        this.quiz = await quizResponse.json();
        this.quizType = this.quiz.quizType || 'TYPING';

        if (this.quizType === 'FILL_BLANK') {
            await this.loadFillBlankQuiz();
        } else {
            const answersResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}/answers`, {
                headers: this.getAuthHeaders()
            });
            if (!answersResponse.ok) {
                throw new Error('加载答案失败');
            }
            this.answers = await answersResponse.json();
        }

        // 重置UI状态
        this.resetQuizUI();

        // 恢复分组进度（如果有）
        if (this.groupMode) {
            const { completed, isGiveUp } = this.restoreQuizProgress(quizId);
            if (completed) {
                // 如果已完成，显示结果面板
                this.showCompletedQuizResult(isGiveUp);
            }
        }

        // 根据测验类型渲染UI
        this.renderQuizTypeUI();

        // 渲染测验信息
        this.renderQuizInfo();

        // 复习模式：渲染复习进度
        if (this.groupMode || this.isReviewMode) {
            this.renderGroupProgress();
        }

        // 重启计时器
        this.startTimer();
        this.isQuizActive = true;
    }

    /**
     * 根据测验类型渲染UI
     */
    renderQuizTypeUI() {
        const answersGrid = document.getElementById('answers-grid');
        const fillBlankSection = document.getElementById('fill-blank-section');
        const inputSection = document.getElementById('input-section');

        if (this.quizType === 'FILL_BLANK') {
            // 隐藏打字题UI，显示填空题UI
            answersGrid.style.display = 'none';
            fillBlankSection.style.display = 'block';
            this.renderFillBlankQuiz();
        } else {
            // 显示打字题UI
            // 清除可能残留的内联样式，确保使用 CSS 中的 grid 布局
            answersGrid.style.display = '';
            answersGrid.style.gridTemplateColumns = '';
            answersGrid.style.gap = '';
            fillBlankSection.style.display = 'none';
            UIRenderer.renderAnswersGrid(this.answers, this.foundAnswers);
            UIRenderer.updateScore(this.foundAnswers.size, this.answers.length);
        }
    }

    /**
     * 渲染填空题（放弃专用）- 未答题显示红色框
     */
    renderFillBlankQuizForGiveUp(originallyFilledIndices) {
        const questionEl = document.getElementById('fill-blank-question');
        const textEl = document.getElementById('fill-blank-text');

        if (!this.fillBlankQuiz) return;

        // 渲染题目区域
        questionEl.textContent = '题目: ' + (this.quiz.title || '填空题');

        // 按位置从后往前处理，避免索引偏移问题
        const blanks = this.fillBlankQuiz.blanks || [];
        let result = this.fillBlankQuiz.fullText;

        // 按 startIndex 降序排序（从后往前替换）
        const sortedBlanks = blanks.map((blank, index) => ({...blank, originalIndex: index}))
            .sort((a, b) => b.startIndex - a.startIndex);

        sortedBlanks.forEach(item => {
            const originalIndex = item.originalIndex;
            // 原本是未填写的（在放弃时需要标记为红色）
            const wasOriginallyEmpty = !originallyFilledIndices.has(originalIndex);
            const correctAnswer = item.correctAnswer || '';
            const comment = item.comment || '';

            // 为放弃场景创建特殊的wrapper样式
            const wrapperHTML = '<span class="fill-blank-wrapper" ' +
                'data-mode="giveup" ' +
                'data-was-empty="' + (wasOriginallyEmpty ? '1' : '0') + '" ' +
                'data-blank-index="' + originalIndex + '" ' +
                'data-answer="' + encodeURIComponent(correctAnswer) + '" ' +
                'data-correct="' + encodeURIComponent(correctAnswer) + '" ' +
                'data-comment="' + encodeURIComponent(comment) + '"></span>';

            result = result.substring(0, item.startIndex) + wrapperHTML + result.substring(item.endIndex);
        });

        // 将换行符替换为带额外间距的 div
        result = result.replace(/\n/g, '<div class="manual-break"></div>');

        textEl.innerHTML = result;
        this.replaceFillBlankWrappers(textEl);

        // 为放弃模式的空格添加特殊样式
        document.querySelectorAll('.fill-blank-wrapper[data-mode="giveup"]').forEach(wrapper => {
            const wasEmpty = wrapper.getAttribute('data-was-empty') === '1';
            if (wasEmpty) {
                // 未答出的显示红色边框
                wrapper.style.border = '2px solid #ef4444';
                wrapper.style.backgroundColor = '#fee2e2';
            } else {
                // 已答出的显示绿色边框
                wrapper.style.border = '2px solid #22c55e';
                wrapper.style.backgroundColor = '#dcfce7';
            }
            wrapper.style.borderRadius = '4px';
            wrapper.style.padding = '2px 6px';
        });
    }

    /**
     * 渲染填空题
     */
    renderFillBlankQuiz() {
        const questionEl = document.getElementById('fill-blank-question');
        const textEl = document.getElementById('fill-blank-text');

        if (!this.fillBlankQuiz) return;

        // 渲染题目区域
        questionEl.textContent = '题目: ' + (this.quiz.title || '填空题');

        // 按位置从后往前处理，避免索引偏移问题
        const blanks = this.fillBlankQuiz.blanks || [];
        let result = this.fillBlankQuiz.fullText;

        // 按 startIndex 降序排序（从后往前替换）
        const sortedBlanks = blanks.map((blank, index) => ({...blank, originalIndex: index}))
            .sort((a, b) => b.startIndex - a.startIndex);

        sortedBlanks.forEach(item => {
            const isFilled = this.filledBlanks.has(item.originalIndex);
            const correctAnswer = item.correctAnswer || '';
            const comment = item.comment || '';
            const userAnswer = isFilled ? (this.filledBlanks.get(item.originalIndex) || '') : '';

            const wrapperHTML = '<span class="fill-blank-wrapper" ' +
                'data-mode="play" ' +
                'data-filled="' + (isFilled ? '1' : '0') + '" ' +
                'data-blank-index="' + item.originalIndex + '" ' +
                'data-answer="' + encodeURIComponent(userAnswer) + '" ' +
                'data-correct="' + encodeURIComponent(correctAnswer) + '" ' +
                'data-comment="' + encodeURIComponent(comment) + '"></span>';

            result = result.substring(0, item.startIndex) + wrapperHTML + result.substring(item.endIndex);
        });

        // 将换行符替换为带额外间距的 div
        result = result.replace(/\n/g, '<div class="manual-break"></div>');

        textEl.innerHTML = result;
        this.replaceFillBlankWrappers(textEl);
    }

    /**
     * 聚焦填空题输入框
     */
    focusFillBlankInput() {
        document.getElementById('answer-input').focus();
    }

    /**
     * 处理填空题输入
     */
    handleInput(input) {
        if (!input || input.trim() === '') {
            return;
        }

        if (this.quizType === 'FILL_BLANK') {
            this.handleFillBlankInput(input.trim());
        } else {
            this.checkAnswer(input.trim());
        }
    }

    /**
     * 处理填空题输入 - 单个输入框
     */
    handleFillBlankInput(userAnswer) {
        // 检查是否匹配任何未填写的空格
        const blanks = this.fillBlankQuiz.blanks || [];
        
        for (let i = 0; i < blanks.length; i++) {
            // 只检查未填写的空格
            if (this.filledBlanks.has(i)) {
                continue;
            }
            
            const blank = blanks[i];
            // 不区分大小写匹配
            if (userAnswer.toLowerCase() === blank.correctAnswer.toLowerCase()) {
                // 找到匹配的空格
                this.filledBlanks.set(i, userAnswer);
                
                // 显示反馈
                UIRenderer.showFeedback('正确!', 'success');
                this.clearInput();
                
                // 重新渲染填空题
                this.renderFillBlankQuiz();
                this.updateFillBlankScore();

                // 检查是否完成所有填空
                if (this.filledBlanks.size === blanks.length) {
                    this.endQuiz();
                }
                return;
            }
        }
    }

    /**
     * 更新填空题得分
     */
    updateFillBlankScore() {
        const found = this.filledBlanks.size;
        const total = this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0;
        UIRenderer.updateScore(found, total);
    }

    /**
     * 渲染测验信息
     */
    renderQuizInfo() {
        document.getElementById('quiz-title').textContent = this.quiz.title;
        document.getElementById('quiz-description').textContent = this.quiz.description || '';
    }

    /**
     * 渲染复习进度（分组模式或全局复习模式）
     */
    renderGroupProgress() {
        if ((!this.groupMode && !this.isReviewMode) || this.groupQuizzes.length <= 1) return;

        const progressLabel = this.groupMode ? '分组进度' : '今日复习';
        
        // 使用保存的原始总数
        let totalCount = this.groupQuizzes.length;
        if (this.groupMode && this.groupQuizTotal) {
            totalCount = this.groupQuizTotal;
        } else if (this.isReviewMode && this.reviewQuizTotal) {
            totalCount = this.reviewQuizTotal;
        }

        let progressHTML = `
            <div id="group-progress" class="group-progress">
                <div class="group-progress-info">
                    <span>${progressLabel}: ${this.currentQuizIndex + 1} / ${totalCount}</span>
                    <span class="current-quiz-name">${this.quiz.title}</span>
                </div>
                <div class="group-nav-hint">
                    按 ← → 切换测验
                </div>
            </div>
        `;

        // 添加到页面顶部
        const header = document.getElementById('quiz-header');
        const existingProgress = document.getElementById('group-progress');
        if (existingProgress) {
            existingProgress.remove();
        }
        header.insertAdjacentHTML('afterend', progressHTML);
    }

    /**
     * 设置事件监听器
     */
    setupEventListeners() {
        const input = document.getElementById('answer-input');

        // 监听输入事件(实时检查)
        input.addEventListener('input', (e) => {
            if (this.isQuizActive) {
                this.handleInput(e.target.value);
            }
        });

        // 重新开始按钮
        document.getElementById('restart-btn').addEventListener('click', () => {
            location.reload();
        });

        // 复习模式：键盘导航（分组模式和全局复习模式都支持）
        if (this.groupMode || this.isReviewMode) {
            document.addEventListener('keydown', (e) => {
                // 复习模式下，即使测验结束也允许切换
                if (this.groupMode) {
                    // 分组模式下，如果测验还在进行中，不处理导航
                    if (!this.isQuizActive) return;
                }

                if (e.key === 'ArrowLeft') {
                    e.preventDefault();
                    this.navigatePrevQuiz();
                } else if (e.key === 'ArrowRight') {
                    e.preventDefault();
                    this.navigateNextQuiz();
                }
            });
        }
    }

    /**
     * 切换到上一个测验
     */
    navigatePrevQuiz() {
        if (this.currentQuizIndex > 0) {
            // 如果当前测验未完成才保存进度
            if (!this.isQuizCompleted()) {
                this.saveCurrentQuizProgress();
            }
            this.currentQuizIndex--;
            this.loadQuizById(this.groupQuizzes[this.currentQuizIndex].id);
        }
    }

    /**
     * 切换到下一个测验
     */
    navigateNextQuiz() {
        if (this.currentQuizIndex < this.groupQuizzes.length - 1) {
            // 如果当前测验未完成才保存进度
            if (!this.isQuizCompleted()) {
                this.saveCurrentQuizProgress();
            }
            this.currentQuizIndex++;
            this.loadQuizById(this.groupQuizzes[this.currentQuizIndex].id);
        }
    }

    /**
     * 检查当前测验是否已完成
     */
    isQuizCompleted() {
        const progress = this.groupProgress.get(this.quizId);
        return progress && progress.completed;
    }

    /**
     * 保存当前测验进度
     */
    saveCurrentQuizProgress() {
        const totalAnswers = this.answers ? this.answers.length : 0;
        const totalBlanks = this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0;

        const progress = {
            quizId: this.quizId,
            quizTitle: this.quiz.title,
            quizType: this.quizType,
            foundAnswers: Array.from(this.foundAnswers), // 保存已找到的答案ID
            filledBlanks: Object.fromEntries(this.filledBlanks), // 保存填空题进度
            completed: (totalAnswers > 0 && this.foundAnswers.size === totalAnswers) ||
                      (totalBlanks > 0 && this.filledBlanks.size === totalBlanks),
            isGiveUp: false
        };

        console.log('保存当前进度:', progress);
        // 保存或更新进度
        this.groupProgress.set(this.quizId, progress);
    }

    /**
     * 保存放弃状态
     */
    saveGiveUpProgress() {
        const progress = {
            quizId: this.quizId,
            quizTitle: this.quiz.title,
            quizType: this.quizType,
            foundAnswers: Array.from(this.foundAnswers),
            filledBlanks: Object.fromEntries(this.filledBlanks),
            completed: true,
            isGiveUp: true
        };

        console.log('保存放弃状态:', progress);
        this.groupProgress.set(this.quizId, progress);
    }

    /**
     * 恢复测验进度
     */
    restoreQuizProgress(quizId) {
        const progress = this.groupProgress.get(quizId);
        console.log('restoreQuizProgress:', quizId, progress);

        if (!progress) return { completed: false, isGiveUp: false };

        // 恢复打字题进度
        if (progress.foundAnswers && progress.foundAnswers.length > 0) {
            this.foundAnswers = new Set(progress.foundAnswers);
        }

        // 恢复填空题进度
        if (progress.filledBlanks && progress.filledBlanks.size > 0) {
            this.filledBlanks = new Map(progress.filledBlanks);
        }

        console.log('恢复结果:', { completed: progress.completed, isGiveUp: progress.isGiveUp });
        return { completed: progress.completed, isGiveUp: progress.isGiveUp };
    }

    /**
     * 检查答案
     */
    async checkAnswer(input) {
        try {
            const response = await fetch(`${this.apiBase}/answers/validate`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({
                    quizId: this.quizId,
                    input: input
                })
            });

            const result = await response.json();

            if (result.valid) {
                // 检查是否已经找到
                if (this.foundAnswers.has(result.answerId)) {
                    UIRenderer.showFeedback('已回答', 'duplicate');
                    this.clearInput();
                } else {
                    this.markAnswerFound(result.answerId, result.displayContent);
                }
            }
        } catch (error) {
            console.error('验证答案失败:', error);
        }
    }

    /**
     * 标记答案为已找到
     */
    markAnswerFound(answerId, displayContent) {
        this.foundAnswers.add(answerId);
        
        // 更新UI
        UIRenderer.highlightAnswer(answerId);
        UIRenderer.updateScore(this.foundAnswers.size, this.answers.length);
        UIRenderer.showFeedback('正确!', 'success');
        
        // 清空输入框
        this.clearInput();

        // 检查是否完成
        if (this.foundAnswers.size === this.answers.length) {
            this.endQuiz();
        }
    }

    /**
     * 清空输入框
     */
    clearInput() {
        const input = document.getElementById('answer-input');
        input.value = '';
        
        // 清除反馈消息
        setTimeout(() => {
            UIRenderer.showFeedback('', '');
        }, 1000);
    }

    /**
     * 启动计时器
     */
    startTimer() {
        const timeLimit = this.quiz.timeLimit;
        
        this.timer = new TimerModule(timeLimit, () => {
            this.endQuiz();
        });
        
        this.timer.start();
    }

    /**
     * 结束测验
     */
    endQuiz(isGiveUp = false) {
        this.isQuizActive = false;

        if (this.timer) {
            this.timer.stop();
        }

        // 禁用输入框和放弃按钮
        document.getElementById('answer-input').disabled = true;
        document.getElementById('give-up-btn').disabled = true;

        // 禁用填空题输入框
        if (this.quizType === 'FILL_BLANK') {
            document.querySelectorAll('.fill-blank-input').forEach(input => {
                input.disabled = true;
            });
        }

        // 计算统计数据
        let found, total;
        if (this.quizType === 'FILL_BLANK') {
            found = this.filledBlanks.size;
            total = this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0;
        } else {
            found = this.foundAnswers.size;
            total = this.answers.length;
        }

        const stats = {
            found: found,
            total: total,
            accuracy: total > 0 ? Math.round((found / total) * 100) : 0,
            timeElapsed: this.timer ? this.timer.getElapsedTime() : 0,
            quizType: this.quizType,
            isGiveUp: isGiveUp
        };

        // 获取未答出的答案
        let missedAnswers = [];
        if (this.quizType === 'FILL_BLANK') {
            // 如果是放弃，显示所有正确答案
            if (isGiveUp) {
                // 保存原本已填写的空格索引
                const originallyFilledIndices = new Set(this.filledBlanks.keys());

                // 填充所有空格
                this.fillBlankQuiz.blanks.forEach((blank, index) => {
                    this.filledBlanks.set(index, blank.correctAnswer);
                });
                // 重新渲染填空题显示（使用放弃专用方法，让未答题显示红色框）
                this.renderFillBlankQuizForGiveUp(originallyFilledIndices);

                // 所有空格都是"未答出"的（因为是放弃）
                missedAnswers = this.fillBlankQuiz.blanks.map((blank, index) => ({
                    id: index,
                    content: blank.correctAnswer,
                    displayContent: blank.correctAnswer,
                    comment: blank.comment
                }));
            } else {
                // 获取未填写的空格正确答案
                if (this.fillBlankQuiz && this.fillBlankQuiz.blanks) {
                    missedAnswers = this.fillBlankQuiz.blanks
                        .map((blank, idx) => ({...blank, originalIndex: idx}))
                        .filter(item => !this.filledBlanks.has(item.originalIndex))
                        .map(item => ({
                            id: item.originalIndex,
                            content: item.correctAnswer,
                            displayContent: item.correctAnswer,
                            comment: item.comment
                        }));
                }
            }
        } else {
            missedAnswers = this.answers.filter(
                answer => !this.foundAnswers.has(answer.id)
            );

            // 如果是放弃，在答案网格中显示所有答案
            if (isGiveUp) {
                UIRenderer.showAllAnswers(this.answers, this.foundAnswers);
            }
        }

        // 保存当前测验进度（分组模式）
        if (this.groupMode) {
            if (isGiveUp) {
                this.saveGiveUpProgress();
            } else {
                this.saveCurrentQuizProgress();
            }
        }

        // 显示结果
        UIRenderer.showResults(stats, missedAnswers);

        // 复习模式：显示评级面板
        if (typeof isReviewMode !== 'undefined' && isReviewMode) {
            setTimeout(() => {
                showRatingPanel();
            }, 500);
        }

        // 分组模式：显示分组结果汇总
        if (this.groupMode) {
            this.showGroupResultsSummary();
        }
    }

    /**
     * 显示分组结果汇总
     */
    showGroupResultsSummary() {
        const resultsPanel = document.getElementById('results-panel');

        let summaryHTML = `
            <div class="group-results-summary">
                <h3>📊 分组测验结果汇总</h3>
        `;

        let totalFound = 0;
        let totalQuestions = 0;

        this.groupProgress.forEach((progress, quizId) => {
            const found = progress.foundAnswers ? progress.foundAnswers.length : 0;
            const total = progress.quizType === 'FILL_BLANK'
                ? (progress.filledBlanks ? progress.filledBlanks.size : 0)
                : this.groupQuizzes.find(q => q.id === quizId)?.totalAnswers || 0;

            totalFound += found;
            totalQuestions += total;
            const accuracy = total > 0 ? Math.round((found / total) * 100) : 0;
            summaryHTML += `
                <div class="group-result-item">
                    <span class="quiz-name">${progress.quizTitle}</span>
                    <span class="quiz-score">${found}/${total} (${accuracy}%)</span>
                </div>
            `;
        });

        // 添加总分
        const overallAccuracy = totalQuestions > 0 ? Math.round((totalFound / totalQuestions) * 100) : 0;
        summaryHTML += `
                <div class="group-result-item" style="background: #667eea; color: white; margin-top: 15px;">
                    <span class="quiz-name" style="color: white;">总分</span>
                    <span class="quiz-score" style="color: white;">${totalFound}/${totalQuestions} (${overallAccuracy}%)</span>
                </div>
            </div>
        `;

        resultsPanel.insertAdjacentHTML('beforeend', summaryHTML);
    }

    /**
     * 显示所有填空题正确答案
     */
    showAllFillBlankAnswers() {
        if (!this.fillBlankQuiz) return;

        // 填充所有空格
        this.fillBlankQuiz.blanks.forEach((blank, index) => {
            if (!this.filledBlanks.has(index)) {
                this.filledBlanks.set(index, blank.correctAnswer);
            }
        });

        // 重新渲染填空题显示
        this.renderFillBlankQuiz();
    }

    /**
     * 放弃测验
     */
    giveUp() {
        if (!confirm('确定要放弃吗?将显示所有答案。')) {
            return;
        }
        this.endQuiz(true);
    }
}
