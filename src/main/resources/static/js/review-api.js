/**
 * 复习系统 API 封装
 * 提供间隔重复复习相关的所有API调用
 */

const ReviewAPI = {
    /**
     * 获取今日复习概览统计
     */
    async getTodaySummary() {
        return api.get('/review/today');
    },

    /**
     * 获取所有待复习的项目列表
     */
    async getDueItems() {
        return api.get('/review/quizzes');
    },

    /**
     * 获取分组复习摘要列表
     */
    async getGroupSummary() {
        return api.get('/review/groups/summary');
    },

    /**
     * 获取指定分组下的测验复习列表
     * @param {number} groupId - 分组ID
     */
    async getGroupReviewItems(groupId) {
        return api.get(`/review/groups/${groupId}/quizzes`);
    },

    /**
     * 获取下一个待学习/复习的测验
     * @param {number} groupId - 分组ID（可选）
     * @param {number} currentQuizId - 当前正在做的测验ID（可选，用于排除）
     */
    async getNextQuiz(groupId = null, currentQuizId = null) {
        let url = '/review/next';
        const params = [];
        if (groupId) params.push(`groupId=${groupId}`);
        if (currentQuizId) params.push(`currentQuizId=${currentQuizId}`);
        if (params.length > 0) url += '?' + params.join('&');
        return api.get(url);
    },

    /**
     * 提交学习评级
     * @param {number} quizId - 测验ID
     * @param {number} rating - 评级 (1=重来, 2=困难, 3=良好, 4=简单)
     * @param {number} groupId - 分组ID（可选）
     */
    async submitLearnRating(quizId, rating, groupId = null) {
        const body = { rating };
        if (groupId) body.groupId = parseInt(groupId);
        return api.post(`/review/${quizId}/learn`, body);
    },

    /**
     * 提交复习评级
     * @param {number} quizId - 测验ID
     * @param {number} rating - 评级 (1=重来, 2=困难, 3=良好, 4=简单)
     * @param {number} groupId - 分组ID（可选）
     */
    async submitReviewRating(quizId, rating, groupId = null) {
        const body = { rating };
        if (groupId) body.groupId = parseInt(groupId);
        return api.post(`/review/${quizId}/review`, body);
    },

    /**
     * 获取测验的复习状态
     * @param {number} quizId - 测验ID
     */
    async getQuizStatus(quizId) {
        return api.get(`/review/${quizId}/status`);
    },

    /**
     * 搁置测验（延后复习）
     * @param {number} quizId - 测验ID
     * @param {number} days - 搁置天数，默认1天
     */
    async buryQuiz(quizId, days = 1) {
        return api.post(`/review/${quizId}/bury`, { days });
    },

    /**
     * 暂停/恢复测验复习
     * @param {number} quizId - 测验ID
     */
    async suspendQuiz(quizId) {
        return api.post(`/review/${quizId}/suspend`);
    },

    /**
     * 重置测验复习状态
     * @param {number} quizId - 测验ID
     */
    async resetQuiz(quizId) {
        return api.post(`/review/${quizId}/reset`);
    },

    /**
     * 按状态筛选测验
     * @param {string} status - 状态 (NEW/LEARNING/REVIEW/RELEARNING/SUSPENDED)
     */
    async filterByStatus(status) {
        return api.get(`/review/filter?status=${status}`);
    },

    /**
     * 按分组筛选测验
     * @param {number} groupId - 分组ID
     */
    async filterByGroup(groupId) {
        return api.get(`/review/filter?groupId=${groupId}`);
    },

    /**
     * 获取搁置的测验列表
     */
    async getBuriedQuizzes() {
        return api.get('/review/buried');
    },

    /**
     * 获取复习统计数据
     */
    async getStats() {
        return api.get('/review/stats');
    },

    /**
     * 获取学习状态（今日待学习数量等）
     */
    async getLearningStatus() {
        return api.get('/learning/status');
    },

    /**
     * 开始学习指定测验
     * @param {number} quizId - 测验ID
     */
    async startLearning(quizId) {
        return api.post(`/learning/${quizId}/start`);
    },

    /**
     * 跳过当前学习测验
     */
    async skipLearning() {
        return api.post('/learning/skip');
    }
};

/**
 * 模式切换管理
 * 管理自由练习模式和Anki复习模式的切换
 */
const ModeManager = {
    MODE_KEY: 'typingquiz_mode',
    MODES: {
        PRACTICE: 'practice',
        REVIEW: 'review'
    },

    /**
     * 获取当前模式
     * @returns {string} 'practice' 或 'review'
     */
    getCurrentMode() {
        return localStorage.getItem(this.MODE_KEY) || this.MODES.PRACTICE;
    },

    /**
     * 设置当前模式
     * @param {string} mode - 'practice' 或 'review'
     */
    setMode(mode) {
        if (Object.values(this.MODES).includes(mode)) {
            localStorage.setItem(this.MODE_KEY, mode);
            this.updateUI();
        }
    },

    /**
     * 切换到练习模式
     */
    switchToPractice() {
        this.setMode(this.MODES.PRACTICE);
    },

    /**
     * 切换到复习模式
     */
    switchToReview() {
        this.setMode(this.MODES.REVIEW);
    },

    /**
     * 检查当前是否为复习模式
     */
    isReviewMode() {
        return this.getCurrentMode() === this.MODES.REVIEW;
    },

    /**
     * 更新页面上的模式切换UI
     */
    updateUI() {
        const mode = this.getCurrentMode();
        document.querySelectorAll('.mode-switcher-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.mode === mode);
        });
    },

    /**
     * 获取模式显示名称
     */
    getModeDisplayName(mode = null) {
        const m = mode || this.getCurrentMode();
        return m === this.MODES.REVIEW ? 'Anki复习' : '自由练习';
    }
};
