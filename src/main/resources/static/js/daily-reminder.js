/**
 * 每日复习提醒服务
 * 在首页显示今日复习提醒
 */
const DailyReminder = {
    STORAGE_KEY: 'last_review_reminder',
    REMINDER_SHOWN_KEY: 'reminder_shown_today',

    /**
     * 检查并显示每日提醒
     */
    async checkAndShow() {
        const today = new Date().toDateString();
        const lastShown = localStorage.getItem(this.REMINDER_SHOWN_KEY);

        // 如果今天已经显示过，不再显示
        if (lastShown === today) {
            return;
        }

        try {
            const token = localStorage.getItem('typingquiz_token');
            if (!token) return;

            // 获取今日复习数量
            const response = await fetch('/api/review/today-summary', {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) return;

            const summary = await response.json();
            const totalDue = (summary.newCount || 0) + (summary.learningCount || 0) +
                            (summary.reviewCount || 0) + (summary.relearningCount || 0);

            if (totalDue > 0) {
                this.showReminderModal(totalDue);
                localStorage.setItem(this.REMINDER_SHOWN_KEY, today);
            }
        } catch (error) {
            console.error('检查每日提醒失败:', error);
        }
    },

    /**
     * 显示提醒弹窗
     */
    showReminderModal(count) {
        // 创建弹窗元素
        const modal = document.createElement('div');
        modal.id = 'daily-reminder-modal';
        modal.innerHTML = `
            <div class="reminder-overlay">
                <div class="reminder-modal">
                    <div class="reminder-icon">📚</div>
                    <h3 class="reminder-title">今日有 ${count} 个测验待复习</h3>
                    <p class="reminder-message">保持学习的连续性，现在就开始复习吧！</p>
                    <div class="reminder-actions">
                        <button class="reminder-btn primary" onclick="DailyReminder.startReview()">
                            开始复习
                        </button>
                        <button class="reminder-btn secondary" onclick="DailyReminder.dismiss()">
                            稍后再说
                        </button>
                    </div>
                </div>
            </div>
        `;

        // 添加样式
        const style = document.createElement('style');
        style.textContent = `
            .reminder-overlay {
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 1000;
                animation: fadeIn 0.3s ease;
            }

            .reminder-modal {
                background: white;
                padding: 32px;
                border-radius: 16px;
                text-align: center;
                max-width: 360px;
                box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
                animation: slideUp 0.3s ease;
            }

            .reminder-icon {
                font-size: 3rem;
                margin-bottom: 16px;
            }

            .reminder-title {
                font-size: 1.3rem;
                color: #312E81;
                margin-bottom: 8px;
            }

            .reminder-message {
                color: #64748B;
                margin-bottom: 24px;
            }

            .reminder-actions {
                display: flex;
                gap: 12px;
                justify-content: center;
            }

            .reminder-btn {
                padding: 12px 24px;
                border: none;
                border-radius: 8px;
                font-size: 1rem;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.2s ease;
            }

            .reminder-btn.primary {
                background: #4F46E5;
                color: white;
            }

            .reminder-btn.primary:hover {
                background: #6366F1;
            }

            .reminder-btn.secondary {
                background: #F3F4F6;
                color: #6B7280;
            }

            .reminder-btn.secondary:hover {
                background: #E5E7EB;
            }

            @keyframes fadeIn {
                from { opacity: 0; }
                to { opacity: 1; }
            }

            @keyframes slideUp {
                from { transform: translateY(20px); opacity: 0; }
                to { transform: translateY(0); opacity: 1; }
            }
        `;

        document.head.appendChild(style);
        document.body.appendChild(modal);
    },

    /**
     * 开始复习
     */
    startReview() {
        this.dismiss();
        window.location.href = 'review.html';
    },

    /**
     * 关闭提醒
     */
    dismiss() {
        const modal = document.getElementById('daily-reminder-modal');
        if (modal) {
            modal.remove();
        }
    }
};

// 页面加载时检查提醒
if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        // 只在首页显示提醒
        if (window.location.pathname.includes('home.html') ||
            window.location.pathname === '/' ||
            window.location.pathname.endsWith('/')) {
            DailyReminder.checkAndShow();
        }
    });
}
