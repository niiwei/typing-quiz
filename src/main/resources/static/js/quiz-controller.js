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

        // 加载答案列表
        const answersResponse = await fetch(`${this.apiBase}/quizzes/${this.quizId}/answers`);
        if (!answersResponse.ok) {
            throw new Error('加载答案失败');
        }
        this.answers = await answersResponse.json();

        // 渲染UI
        this.renderQuizInfo();
        UIRenderer.renderAnswersGrid(this.answers, this.foundAnswers);
        UIRenderer.updateScore(this.foundAnswers.size, this.answers.length);
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
     * 处理输入
     */
    handleInput(input) {
        if (!input || input.trim() === '') {
            return;
        }

        this.checkAnswer(input.trim());
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

        // 计算统计数据
        const stats = {
            found: this.foundAnswers.size,
            total: this.answers.length,
            accuracy: Math.round((this.foundAnswers.size / this.answers.length) * 100),
            timeElapsed: this.timer ? this.timer.getElapsedTime() : 0,
            isGiveUp: isGiveUp
        };

        // 获取未答出的答案
        const missedAnswers = this.answers.filter(
            answer => !this.foundAnswers.has(answer.id)
        );

        // 如果是放弃,在答案网格中显示所有答案
        if (isGiveUp) {
            UIRenderer.showAllAnswers(this.answers, this.foundAnswers);
        }

        // 显示结果
        UIRenderer.showResults(stats, missedAnswers);
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
