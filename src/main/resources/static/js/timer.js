/**
 * TimerModule - 计时器模块
 * 支持倒计时和正计时两种模式
 */
class TimerModule {
    constructor(timeLimit, onExpire) {
        this.timeLimit = timeLimit; // null表示无限制
        this.elapsed = 0;
        this.interval = null;
        this.onExpire = onExpire;
        this.startTime = null;
    }

    /**
     * 启动计时器
     */
    start() {
        this.startTime = Date.now();
        
        this.interval = setInterval(() => {
            this.elapsed = Math.floor((Date.now() - this.startTime) / 1000);
            this.updateDisplay();

            // 检查是否到期
            if (this.timeLimit && this.elapsed >= this.timeLimit) {
                this.stop();
                if (this.onExpire) {
                    this.onExpire();
                }
            }
        }, 1000);

        // 立即更新一次显示
        this.updateDisplay();
    }

    /**
     * 停止计时器
     */
    stop() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
    }

    /**
     * 获取已用时间(秒)
     */
    getElapsedTime() {
        return this.elapsed;
    }

    /**
     * 获取剩余时间(秒)
     */
    getRemainingTime() {
        if (!this.timeLimit) {
            return null;
        }
        return Math.max(0, this.timeLimit - this.elapsed);
    }

    /**
     * 更新显示
     */
    updateDisplay() {
        const display = document.getElementById('time-display');
        
        if (this.timeLimit) {
            // 倒计时模式
            const remaining = this.getRemainingTime();
            display.textContent = this.formatTime(remaining);
            
            // 时间不足时变红
            if (remaining <= 60) {
                display.style.color = '#dc3545';
            }
        } else {
            // 正计时模式
            display.textContent = this.formatTime(this.elapsed);
        }
    }

    /**
     * 格式化时间为 MM:SS
     */
    formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
}
