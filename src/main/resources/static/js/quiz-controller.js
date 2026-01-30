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

        // 分组答题模式
        this.groupMode = false;
        this.groupId = null;
        this.groupQuizzes = []; // 分组中的所有测验
        this.currentQuizIndex = 0; // 当前测验索引
        this.groupProgress = new Map(); // 存储各测验的进度 {quizId -> {foundAnswers, filledBlanks, completed}}
    }

    /**
     * 初始化测验
     */
    async init() {
        try {
            // 检查是否是分组模式
            this.checkGroupMode();

            if (this.groupMode) {
                await this.loadGroupQuizzes();
            } else {
                await this.loadQuiz();
            }

            this.setupEventListeners();
            this.startTimer();
            this.isQuizActive = true;

            // 聚焦输入框
            document.getElementById('answer-input').focus();
        } catch (error) {
            console.error('初始化失败:', error);
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
    }

    /**
     * 加载分组测验列表
     */
    async loadGroupQuizzes() {
        const response = await fetch(`${this.apiBase}/groups/${this.groupId}/quizzes`);
        if (!response.ok) {
            throw new Error('加载分组测验失败');
        }
        this.groupQuizzes = await response.json();

        if (this.groupQuizzes.length === 0) {
            throw new Error('该分组中没有测验');
        }

        // 加载第一个测验
        this.currentQuizIndex = 0;
        await this.loadQuizById(this.groupQuizzes[0].id);
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

        const quizResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}`);
        if (!quizResponse.ok) {
            throw new Error('测验不存在');
        }
        this.quiz = await quizResponse.json();
        this.quizType = this.quiz.quizType || 'TYPING';

        if (this.quizType === 'FILL_BLANK') {
            await this.loadFillBlankQuiz();
        } else {
            const answersResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}/answers`);
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
                return;
            }
        }

        this.renderQuizInfo();
        this.renderQuizTypeUI();
        this.renderGroupProgress();

        // 启动新计时器
        this.startTimer();
    }

    /**
     * 显示已完成测验的结果
     */
    showCompletedQuizResult(isGiveUp = false) {
        // 计算统计
        let found, total, missedAnswers = [];
        if (this.quizType === 'FILL_BLANK') {
            found = this.filledBlanks.size;
            total = this.fillBlankQuiz ? this.fillBlankQuiz.blanksCount : 0;

            if (isGiveUp && this.fillBlankQuiz && this.fillBlankQuiz.blanks) {
                // 放弃时显示所有正确答案
                missedAnswers = this.fillBlankQuiz.blanks.map((blank, index) => ({
                    id: index,
                    content: blank.correctAnswer,
                    displayContent: blank.correctAnswer,
                    comment: blank.comment
                }));
            }
        } else {
            found = this.foundAnswers.size;
            total = this.answers ? this.answers.length : 0;

            if (isGiveUp && this.answers) {
                // 放弃时显示所有未答出的答案
                missedAnswers = this.answers.filter(
                    answer => !this.foundAnswers.has(answer.id)
                );
            }
        }

        const stats = {
            found: found,
            total: total,
            accuracy: total > 0 ? Math.round((found / total) * 100) : 0,
            timeElapsed: 0,
            quizType: this.quizType,
            isGiveUp: isGiveUp
        };

        // 先渲染测验UI（显示题目和答案网格）
        this.renderQuizInfo();
        if (this.answers && this.answers.length > 0) {
            this.renderQuizTypeUI();
        }

        // 显示结果面板
        UIRenderer.showResults(stats, missedAnswers);

        // 放弃时在答案网格中显示所有答案
        if (isGiveUp && this.quizType === 'TYPING' && this.answers) {
            UIRenderer.showAllAnswers(this.answers, this.foundAnswers);
        }

        this.renderGroupProgress();
    }

    /**
     * 重置测验UI状态
     */
    resetQuizUI() {
        // 启用输入框和放弃按钮
        const answerInput = document.getElementById('answer-input');
        const giveUpBtn = document.getElementById('give-up-btn');
        const inputSection = document.getElementById('input-section');

        if (answerInput) {
            answerInput.disabled = false;
            answerInput.value = '';
        }

        if (giveUpBtn) {
            giveUpBtn.disabled = false;
        }

        // 显示输入区域（UIRenderer.showResults会隐藏它）
        if (inputSection) {
            inputSection.style.display = '';
        }

        // 隐藏结果面板
        const resultsPanel = document.getElementById('results-panel');
        if (resultsPanel) {
            resultsPanel.style.display = 'none';
        }

        const feedbackMsg = document.getElementById('feedback-message');
        if (feedbackMsg) {
            feedbackMsg.textContent = '';
            feedbackMsg.className = '';
        }

        // 启用填空题输入框
        document.querySelectorAll('.fill-blank-input').forEach(input => {
            input.disabled = false;
        });

        // 重置测验状态
        this.isQuizActive = true;
    }

    /**
     * 从API加载测验数据
     */
    async loadQuiz() {
        // 加载测验详情
        const quizResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}`);
        if (!quizResponse.ok) {
            throw new Error('测验不存在');
        }
        this.quiz = await quizResponse.json();
        this.quizType = this.quiz.quizType || 'TYPING';

        // 根据测验类型加载不同的数据
        if (this.quizType === 'FILL_BLANK') {
            // 加载填空题数据
            await this.loadFillBlankQuiz();
        } else {
            // 加载打字题答案列表
            const answersResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}/answers`);
            if (!answersResponse.ok) {
                throw new Error('加载答案失败');
            }
            this.answers = await answersResponse.json();
        }

        // 渲染UI
        this.renderQuizInfo();
        this.renderQuizTypeUI();
    }

    /**
     * 加载填空题数据
     */
    async loadFillBlankQuiz() {
        const response = await fetch(`${this.apiBase}/fill-blank/quiz/${this.quizId}`);
        if (!response.ok) {
            throw new Error('加载填空题数据失败');
        }
        this.fillBlankQuiz = await response.json();
        
        // 初始化已填写的空格
        this.filledBlanks = new Map();
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
            const userAnswer = this.filledBlanks.get(item.originalIndex) || '___';
            
            const placeholderClass = isFilled ? 'filled' : 'missed';
            const placeholderHTML = '<span class="fill-blank-placeholder ' + placeholderClass + 
                '" data-blank-index="' + item.originalIndex + 
                '" onclick="controller.focusFillBlankInput()">' + userAnswer + '</span>';
            
            result = result.substring(0, item.startIndex) + placeholderHTML + result.substring(item.endIndex);
        });
        
        textEl.innerHTML = result;
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
     * 渲染分组进度
     */
    renderGroupProgress() {
        if (!this.groupMode || this.groupQuizzes.length <= 1) return;

        let progressHTML = `
            <div id="group-progress" class="group-progress">
                <div class="group-progress-info">
                    <span>分组进度: ${this.currentQuizIndex + 1} / ${this.groupQuizzes.length}</span>
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

        // 分组模式：键盘导航
        if (this.groupMode) {
            document.addEventListener('keydown', (e) => {
                // 分组模式下，即使测验结束也允许切换
                if (!this.groupMode) {
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
                headers: {
                    'Content-Type': 'application/json'
                },
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
                // 填充所有空格
                this.fillBlankQuiz.blanks.forEach((blank, index) => {
                    this.filledBlanks.set(index, blank.correctAnswer);
                });
                // 重新渲染填空题显示
                this.renderFillBlankQuiz();

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
