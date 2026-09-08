/**
 * 轻量级埋点追踪库
 * 用于记录用户行为事件和页面访问
 */
const Track = {
    // 配置
    queue: [],
    maxQueueSize: 10,
    flushInterval: 5000, // 5秒自动上报
    sessionId: null,
    userId: null,
    apiBaseUrl: '',

    /**
     * 初始化埋点库
     */
    init() {
        // 从localStorage获取或生成sessionId
        this.sessionId = localStorage.getItem('track_session_id');
        if (!this.sessionId) {
            this.sessionId = 'sess_' + Math.random().toString(36).substr(2, 9) + '_' + Date.now();
            localStorage.setItem('track_session_id', this.sessionId);
        }

        // 获取用户ID
        this.userId = localStorage.getItem('userId');

        // 设置API基础URL
        this.apiBaseUrl = window.location.origin;

        // 启动定时上报
        setInterval(() => this.flush(), this.flushInterval);

        // 页面卸载前上报
        window.addEventListener('beforeunload', () => this.flush());

        // 自动上报页面访问
        this.trackPageView();

        console.log('[Track] 埋点库初始化完成, sessionId:', this.sessionId);
    },

    /**
     * 记录事件
     * @param {string} eventType - 事件类型
     * @param {object} data - 事件数据
     */
    event(eventType, data = {}) {
        this.queue.push({
            userId: this.userId ? parseInt(this.userId) : null,
            eventType: eventType,
            data: {
                ...data,
                url: window.location.pathname,
                ts: Date.now()
            },
            sessionId: this.sessionId,
            pagePath: window.location.pathname
        });

        // 达到阈值立即上报
        if (this.queue.length >= this.maxQueueSize) {
            this.flush();
        }
    },

    /**
     * 记录页面访问
     */
    trackPageView() {
        const data = {
            pagePath: window.location.pathname,
            referrer: document.referrer || '',
            userAgent: navigator.userAgent,
            screenSize: `${window.innerWidth}x${window.innerHeight}`
        };
        this.event('page_view', data);
    },

    /**
     * 记录会话开始
     */
    trackSessionStart() {
        const data = {
            entryPage: window.location.pathname,
            source: this.getSource(),
            deviceType: this.getDeviceType()
        };
        this.event('user_session_start', data);
    },

    /**
     * 记录复习评级提交
     * @param {number} quizId - 测验ID
     * @param {number} rating - 评级(1-4)
     * @param {number} timeSpent - 耗时(秒)
     */
    trackReviewRating(quizId, rating, timeSpent) {
        this.event('review_rating_submit', {
            quizId: quizId,
            rating: rating,
            timeSpent: timeSpent
        });
    },

    /**
     * 记录复习会话开始
     * @param {string} mode - 模式(learning/review/mixed)
     * @param {number} newCount - 新测验数量
     * @param {number} reviewCount - 复习数量
     * @param {number} relearningCount - 重学数量
     */
    trackReviewSessionStart(mode, newCount, reviewCount, relearningCount) {
        this.event('review_session_start', {
            mode: mode,
            newCount: newCount,
            reviewCount: reviewCount,
            relearningCount: relearningCount
        });
    },

    /**
     * 记录复习会话结束
     * @param {number} totalCards - 总卡片数
     * @param {number} totalTimeSec - 总耗时(秒)
     * @param {string} exitType - 退出类型(complete/manual/timeout)
     */
    trackReviewSessionEnd(totalCards, totalTimeSec, exitType) {
        this.event('review_session_end', {
            totalCards: totalCards,
            totalTimeSec: totalTimeSec,
            exitType: exitType
        });
        // 立即上报会话结束事件
        this.flush();
    },

    /**
     * 批量上报事件
     */
    flush() {
        if (this.queue.length === 0) return;

        const events = [...this.queue];
        this.queue = [];

        // 使用sendBeacon确保数据发送（页面卸载时也能发送）
        const url = `${this.apiBaseUrl}/api/track`;
        const data = JSON.stringify(events);

        if (navigator.sendBeacon) {
            navigator.sendBeacon(url, new Blob([data], { type: 'application/json' }));
        } else {
            // 降级使用fetch
            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: data,
                keepalive: true
            }).catch(err => console.error('[Track] 上报失败:', err));
        }
    },

    /**
     * 获取流量来源
     */
    getSource() {
        const referrer = document.referrer;
        if (!referrer) return 'direct';
        if (referrer.includes('google')) return 'google';
        if (referrer.includes('baidu')) return 'baidu';
        if (referrer.includes('wechat') || referrer.includes('wx')) return 'wechat';
        return 'referral';
    },

    /**
     * 获取设备类型
     */
    getDeviceType() {
        const width = window.innerWidth;
        if (width < 768) return 'mobile';
        if (width < 1024) return 'tablet';
        return 'desktop';
    },

    /**
     * 更新用户ID（登录后调用）
     * @param {number} userId - 用户ID
     */
    setUserId(userId) {
        this.userId = userId;
        localStorage.setItem('userId', userId);
    }
};

// 自动初始化
Track.init();

// 导出供全局使用
window.Track = Track;
