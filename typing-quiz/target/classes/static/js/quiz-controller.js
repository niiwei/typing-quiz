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
    }

    /**
     * 初始化测验
     */
    async init() {
        try {
            await this.loadQuiz();
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

        // 显示结果
        UIRenderer.showResults(stats, missedAnswers);
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
