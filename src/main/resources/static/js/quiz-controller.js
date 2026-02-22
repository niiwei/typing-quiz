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
        this.currentQuizStatus = null;
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
                // 分组模式
                await this.loadGroupQuizzes();
                const storedList = sessionStorage.getItem(`groupQuizList_${this.groupId}`);
                let quizList = storedList ? JSON.parse(storedList) : this.groupQuizzes.map(q => q.id);
                this.groupQuizTotal = quizList.length;
                const currentIndex = quizList.indexOf(Number(this.quizId));
                
                if (currentIndex >= 0) {
                    this.currentQuizIndex = currentIndex;
                    await this.loadQuizById(this.quizId);
                } else if (this.groupQuizzes.length > 0) {
                    this.currentQuizIndex = quizList.length - this.groupQuizzes.length;
                    await this.loadQuizById(this.groupQuizzes[0].id);
                } else {
                    sessionStorage.removeItem(`groupQuizList_${this.groupId}`);
                    alert('该分组复习任务已全部完成！');
                    window.location.href = 'home.html';
                    return;
                }
            } else if (this.isReviewMode) {
                // 全局复习模式
                await this.loadGlobalReviewQuizzes();
                const storedList = sessionStorage.getItem('reviewQuizList');
                let quizList = storedList ? JSON.parse(storedList) : this.groupQuizzes.map(q => q.id);
                this.reviewQuizTotal = quizList.length;
                const currentIndex = quizList.indexOf(Number(this.quizId));

                if (currentIndex >= 0) {
                    this.currentQuizIndex = currentIndex;
                    await this.loadQuizById(this.quizId);
                } else if (this.groupQuizzes.length > 0) {
                    this.currentQuizIndex = quizList.length - this.groupQuizzes.length;
                    await this.loadQuizById(this.groupQuizzes[0].id);
                } else {
                    sessionStorage.removeItem('reviewQuizList');
                    alert('今日复习任务已全部完成！');
                    window.location.href = 'home.html';
                    return;
                }
            } else {
                await this.loadQuizById(this.quizId);
            }

            this.setupEventListeners();
            this.startTimer();
            this.isQuizActive = true;

            // 聚焦输入框
            const input = document.getElementById('answer-input');
            if (input) input.focus();
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
     */
    async loadGroupQuizzes() {
        if (this.isReviewMode) {
            const response = await fetch(`${this.apiBase}/review/groups/${this.groupId}/quizzes`, {
                headers: this.getAuthHeaders()
            });
            if (!response.ok) throw new Error('加载分组复习列表失败');
            const items = await response.json();
            const dueItems = items.filter(item => item.label === 'PENDING_LEARN' || item.label === 'PENDING_REVIEW');
            this.groupQuizzes = dueItems.map(item => ({ id: item.quizId, title: item.quizTitle }));
        } else {
            const response = await fetch(`${this.apiBase}/groups/${this.groupId}/quizzes`, {
                headers: this.getAuthHeaders()
            });
            if (!response.ok) throw new Error('加载分组测验失败');
            this.groupQuizzes = await response.json();
        }
        if (this.groupQuizzes.length === 0) throw new Error('该分组中没有测验');
    }

    /**
     * 加载全局待复习测验列表
     */
    async loadGlobalReviewQuizzes() {
        const response = await fetch(`${this.apiBase}/review/quizzes`, {
            headers: this.getAuthHeaders()
        });
        if (!response.ok) throw new Error('加载今日复习列表失败');
        const items = await response.json();
        const dueItems = items.filter(item => item.label === 'PENDING_LEARN' || item.label === 'PENDING_REVIEW');
        this.groupQuizzes = dueItems.map(item => ({ id: item.quizId, title: item.quizTitle }));
        if (this.groupQuizzes.length === 0) throw new Error('今日没有待复习测验');
    }

    /**
     * 重置测验UI状态
     */
    resetQuizUI() {
        this.foundAnswers.clear();
        this.filledBlanks.clear();
        const input = document.getElementById('answer-input');
        if (input) {
            input.value = '';
            input.disabled = false;
        }
        const giveUpBtn = document.getElementById('give-up-btn');
        if (giveUpBtn) giveUpBtn.disabled = false;
        UIRenderer.showFeedback('', '');
    }

    /**
     * 加载填空题数据
     */
    async loadFillBlankQuiz() {
        const response = await fetch(`${this.apiBase}/fill-blank/quiz/${this.quizId}`, {
            headers: this.getAuthHeaders()
        });
        if (!response.ok) throw new Error('加载填空题数据失败');
        this.fillBlankQuiz = await response.json();
    }

    /**
     * 根据ID加载测验
     */
    async loadQuizById(quizId) {
        if (this.timer) this.timer.stop();
        this.quizId = quizId;
        this.foundAnswers.clear();
        this.filledBlanks.clear();

        const quizResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}`, {
            headers: this.getAuthHeaders()
        });
        if (!quizResponse.ok) throw new Error('测验不存在');
        this.quiz = await quizResponse.json();
        this.quizType = this.quiz.quizType || 'TYPING';

        if (this.quizType === 'FILL_BLANK') {
            await this.loadFillBlankQuiz();
        } else {
            const answersResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}/answers`, {
                headers: this.getAuthHeaders()
            });
            if (!answersResponse.ok) throw new Error('加载答案失败');
            this.answers = await answersResponse.json();
        }

        this.resetQuizUI();
        this.renderQuizTypeUI();
        this.renderQuizInfo();
        if (this.groupMode || this.isReviewMode) this.renderGroupProgress();
        this.startTimer();
        this.isQuizActive = true;
    }

    renderQuizTypeUI() {
        const answersGrid = document.getElementById('answers-grid');
        const fillBlankSection = document.getElementById('fill-blank-section');
        if (this.quizType === 'FILL_BLANK') {
            if (answersGrid) answersGrid.style.display = 'none';
            if (fillBlankSection) fillBlankSection.style.display = 'block';
            this.renderFillBlankQuiz();
        } else {
            if (answersGrid) {
                answersGrid.style.display = 'grid';
                UIRenderer.renderAnswersGrid(this.answers, this.foundAnswers);
            }
            if (fillBlankSection) fillBlankSection.style.display = 'none';
            UIRenderer.updateScore(this.foundAnswers.size, this.answers.length);
        }
    }

    renderQuizInfo() {
        const titleEl = document.getElementById('quiz-title');
        const descEl = document.getElementById('quiz-description');
        if (titleEl) titleEl.textContent = this.quiz.title;
        if (descEl) descEl.textContent = this.quiz.description || '';
    }

    renderGroupProgress() {
        const total = this.quizType === 'FILL_BLANK' 
            ? (this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0)
            : (this.answers ? this.answers.length : 0);
        const found = this.quizType === 'FILL_BLANK' ? this.filledBlanks.size : this.foundAnswers.size;
        const progress = total > 0 ? found / total : 0;
        const fillEl = document.getElementById('progress-fill');
        if (fillEl) fillEl.style.width = (progress * 100) + '%';
    }

    renderFillBlankQuiz() {
        const textEl = document.getElementById('fill-blank-text');
        if (!this.fillBlankQuiz || !textEl) return;
        const blanks = this.fillBlankQuiz.blanks || [];
        let result = this.fillBlankQuiz.fullText;
        const sortedBlanks = blanks.map((blank, index) => ({...blank, originalIndex: index}))
            .sort((a, b) => b.startIndex - a.startIndex);

        sortedBlanks.forEach(item => {
            const isFilled = this.filledBlanks.has(item.originalIndex);
            const userAnswer = isFilled ? (this.filledBlanks.get(item.originalIndex) || '') : '';
            const wrapperHTML = `<span class="fill-blank-wrapper" data-mode="play" data-filled="${isFilled ? '1' : '0'}" data-blank-index="${item.originalIndex}" data-answer="${encodeURIComponent(userAnswer)}" data-correct="${encodeURIComponent(item.correctAnswer)}" data-comment="${encodeURIComponent(item.comment || '')}"></span>`;
            result = result.substring(0, item.startIndex) + wrapperHTML + result.substring(item.endIndex);
        });
        textEl.innerHTML = result.replace(/\n/g, '<div class="manual-break"></div>');
        this.replaceFillBlankWrappers(textEl);
    }

    handleInput(input) {
        if (!input || input.trim() === '') return;
        if (this.quizType === 'FILL_BLANK') {
            this.handleFillBlankInput(input.trim());
        } else {
            this.checkAnswer(input.trim());
        }
    }

    handleFillBlankInput(userAnswer) {
        const blanks = this.fillBlankQuiz.blanks || [];
        for (let i = 0; i < blanks.length; i++) {
            if (this.filledBlanks.has(i)) continue;
            if (userAnswer.toLowerCase() === blanks[i].correctAnswer.toLowerCase()) {
                this.filledBlanks.set(i, userAnswer);
                UIRenderer.showFeedback('正确!', 'success');
                this.clearInput();
                this.renderFillBlankQuiz();
                this.updateFillBlankScore();
                if (this.filledBlanks.size === blanks.length) this.endQuiz();
                return;
            }
        }
    }

    updateFillBlankScore() {
        const found = this.filledBlanks.size;
        const total = this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0;
        UIRenderer.updateScore(found, total);
        this.renderGroupProgress();
    }

    async endQuiz(isGiveUp = false) {
        if (!this.isQuizActive) return;
        this.isQuizActive = false;
        if (this.timer) this.timer.stop();

        const input = document.getElementById('answer-input');
        if (input) { input.disabled = true; input.blur(); }

        const timeElapsed = this.timer ? this.timer.getTimeElapsed() : 0;
        const total = this.quizType === 'FILL_BLANK' ? (this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0) : (this.answers ? this.answers.length : 0);
        const found = this.quizType === 'FILL_BLANK' ? (this.filledBlanks ? this.filledBlanks.size : 0) : (this.foundAnswers ? this.foundAnswers.size : 0);
        const accuracy = total > 0 ? Math.round((found / total) * 100) : 0;

        const stats = { quizId: this.quizId, found, total, accuracy, timeElapsed, isGiveUp, quizType: this.quizType };
        let missedAnswers = [];
        if (this.quizType === 'FILL_BLANK') {
            missedAnswers = this.fillBlankQuiz.blanks.filter((_, i) => !this.filledBlanks.has(i)).map(b => ({ content: b.correctAnswer, comment: b.comment }));
        } else {
            missedAnswers = this.answers.filter(a => !this.foundAnswers.has(a.id));
        }

        if (isGiveUp) {
            if (this.quizType === 'FILL_BLANK') this.renderFillBlankQuizForGiveUp(new Set(this.filledBlanks.keys()));
            else UIRenderer.showAllAnswers(this.answers, this.foundAnswers);
        }

        UIRenderer.showResults(stats, missedAnswers);
        if (this.isReviewMode) {
            const panel = document.getElementById('rating-panel');
            if (panel) { panel.style.display = 'block'; await this.loadQuizStatus(); }
        }
        this.saveToLocalHistory(stats);
        this.saveRecordToServer(stats);
    }

    async saveRecordToServer(stats) {
        try {
            await fetch(`${this.apiBase}/quizzes/${this.quizId}/record`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ score: stats.found, totalScore: stats.total, timeElapsed: stats.timeElapsed, accuracy: stats.accuracy, isGiveUp: stats.isGiveUp })
            });
        } catch (e) { console.error('保存记录失败'); }
    }

    saveToLocalHistory(stats) {
        const history = JSON.parse(localStorage.getItem('quiz_history') || '[]');
        history.unshift({ quizId: stats.quizId, title: this.quiz ? this.quiz.title : '未知', accuracy: stats.accuracy, time: UIRenderer.formatTime(stats.timeElapsed), timestamp: Date.now() });
        localStorage.setItem('quiz_history', JSON.stringify(history.slice(0, 20)));
    }

    async loadQuizStatus() {
        try {
            const res = await fetch(`${this.apiBase}/review/quizzes/${this.quizId}/status`, { headers: this.getAuthHeaders() });
            if (res.ok) {
                this.currentQuizStatus = await res.json();
                this.updateRatingIntervals(this.currentQuizStatus);
            }
        } catch (e) { console.error('加载状态失败'); }
    }

    updateRatingIntervals(status) {
        const intervals = this.calculateIntervals(status);
        const selectors = { 1: '.rating-again .interval', 2: '.rating-hard .interval', 3: '.rating-good .interval', 4: '.rating-easy .interval' };
        for (let r in selectors) {
            const el = document.querySelector(selectors[r]);
            if (el) el.textContent = intervals[r];
        }
    }

    calculateIntervals(status) {
        const status_type = status.status;
        const ease = status.easeFactor || 2500;
        const currentInterval = status.intervalDays || 0;
        const learningStep = status.learningStep || 0;
        const MAX_INTERVAL = 365;
        let intervals = {};
        if (status_type === 'NEW' || status_type === 'LEARNING' || status_type === 'RELEARNING') {
            if (learningStep === 0) intervals = { 1: '<10m', 2: '<10m', 3: '10m', 4: '1d' };
            else if (learningStep === 1) intervals = { 1: '<10m', 2: '10m', 3: '1h', 4: '1d' };
            else intervals = { 1: '<10m', 2: '1h', 3: '1d', 4: '4d' };
        } else {
            const format = (d) => d >= 1 ? Math.round(d) + 'd' : Math.round(d * 24) + 'h';
            intervals[1] = '<10m';
            intervals[2] = format(Math.min(MAX_INTERVAL, Math.max(1, currentInterval * 1.2)));
            intervals[3] = format(Math.min(MAX_INTERVAL, Math.max(1, currentInterval * ease / 1000)));
            intervals[4] = format(Math.min(MAX_INTERVAL, Math.max(1, currentInterval * ease / 1000 * 1.3)));
        }
        return intervals;
    }

    setupEventListeners() {
        const input = document.getElementById('answer-input');
        if (input) input.addEventListener('input', (e) => { if (this.isQuizActive) this.handleInput(e.target.value); });
        const restartBtn = document.getElementById('restart-btn');
        if (restartBtn) restartBtn.addEventListener('click', () => location.reload());
        if (this.groupMode || this.isReviewMode) {
            document.addEventListener('keydown', (e) => {
                if (e.key === 'ArrowLeft') { e.preventDefault(); this.navigatePrevQuiz(); }
                else if (e.key === 'ArrowRight') { e.preventDefault(); this.navigateNextQuiz(); }
            });
        }
    }

    navigatePrevQuiz() {
        if (this.currentQuizIndex > 0) {
            this.currentQuizIndex--;
            this.loadQuizById(this.groupQuizzes[this.currentQuizIndex].id);
        }
    }

    navigateNextQuiz() {
        if (this.currentQuizIndex < this.groupQuizzes.length - 1) {
            this.currentQuizIndex++;
            this.loadQuizById(this.groupQuizzes[this.currentQuizIndex].id);
        }
    }

    clearInput() {
        const input = document.getElementById('answer-input');
        if (input) input.value = '';
        setTimeout(() => UIRenderer.showFeedback('', ''), 1000);
    }

    startTimer() {
        const timeLimit = this.quiz.timeLimit;
        this.timer = new TimerModule(timeLimit, () => this.endQuiz());
        this.timer.start();
    }

    giveUp() {
        if (confirm('确定要放弃吗?将显示所有答案。')) this.endQuiz(true);
    }

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

    markAnswerFound(answerId, displayContent) {
        this.foundAnswers.add(answerId);
        UIRenderer.highlightAnswer(answerId);
        UIRenderer.updateScore(this.foundAnswers.size, this.answers.length);
        UIRenderer.showFeedback('正确!', 'success');
        this.clearInput();
        this.renderGroupProgress();
        if (this.foundAnswers.size === this.answers.length) {
            this.endQuiz();
        }
    }

    renderFillBlankQuizForGiveUp(originallyFilledIndices) {
        const textEl = document.getElementById('fill-blank-text');
        if (!this.fillBlankQuiz || !textEl) return;
        const blanks = this.fillBlankQuiz.blanks || [];
        let result = this.fillBlankQuiz.fullText;
        const sortedBlanks = blanks.map((blank, index) => ({...blank, originalIndex: index}))
            .sort((a, b) => b.startIndex - a.startIndex);

        sortedBlanks.forEach(item => {
            const wasOriginallyEmpty = !originallyFilledIndices.has(item.originalIndex);
            const wrapperHTML = `<span class="fill-blank-wrapper" data-mode="giveup" data-was-empty="${wasOriginallyEmpty ? '1' : '0'}" data-blank-index="${item.originalIndex}" data-answer="${encodeURIComponent(item.correctAnswer)}" data-correct="${encodeURIComponent(item.correctAnswer)}" data-comment="${encodeURIComponent(item.comment || '')}"></span>`;
            result = result.substring(0, item.startIndex) + wrapperHTML + result.substring(item.endIndex);
        });
        textEl.innerHTML = result.replace(/\n/g, '<div class="manual-break"></div>');
        this.replaceFillBlankWrappers(textEl);

        document.querySelectorAll('.fill-blank-wrapper[data-mode="giveup"]').forEach(wrapper => {
            const wasEmpty = wrapper.getAttribute('data-was-empty') === '1';
            wrapper.style.border = wasEmpty ? '2px solid #ef4444' : '2px solid #22c55e';
            wrapper.style.backgroundColor = wasEmpty ? '#fee2e2' : '#dcfce7';
            wrapper.style.borderRadius = '4px';
            wrapper.style.padding = '2px 6px';
        });
    }
}
