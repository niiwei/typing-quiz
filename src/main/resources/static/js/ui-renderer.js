/**
 * UIRenderer - UI渲染器
 * 负责所有UI更新和渲染
 */
class UIRenderer {
    /**
     * 渲染答案网格
     */
    static renderAnswersGrid(answers, foundAnswers) {
        const grid = document.getElementById('answers-grid');
        grid.innerHTML = '';

        answers.forEach(answer => {
            const item = document.createElement('div');
            item.className = 'answer-item not-found';
            item.id = `answer-${answer.id}`;
            item.textContent = '?';
            item.dataset.content = answer.content;
            
            if (foundAnswers.has(answer.id)) {
                item.classList.remove('not-found');
                item.classList.add('found');
                item.textContent = answer.content;
            }
            
            grid.appendChild(item);
        });
    }

    /**
     * 高亮已答项
     */
    static highlightAnswer(answerId) {
        const item = document.getElementById(`answer-${answerId}`);
        if (item) {
            item.classList.remove('not-found');
            item.classList.add('found');
            item.textContent = item.dataset.content;
        }
    }

    /**
     * 更新得分显示
     */
    static updateScore(found, total) {
        const display = document.getElementById('score-display');
        display.textContent = `${found}/${total}`;
    }

    /**
     * 显示反馈消息
     */
    static showFeedback(message, type) {
        const feedback = document.getElementById('feedback-message');
        feedback.textContent = message;
        feedback.className = type ? `${type}` : '';
    }

    /**
     * 显示所有答案(放弃时使用)
     */
    static showAllAnswers(answers, foundAnswers) {
        answers.forEach(answer => {
            const item = document.getElementById(`answer-${answer.id}`);
            if (item && !foundAnswers.has(answer.id)) {
                item.classList.remove('not-found');
                item.classList.add('missed');
                item.textContent = answer.content;
            }
        });
    }

    /**
     * 显示最终结果
     */
    static showResults(stats, missedAnswers) {
        // 隐藏输入区域
        document.getElementById('input-section').style.display = 'none';
        
        // 显示结果面板
        const resultsPanel = document.getElementById('results-panel');
        resultsPanel.style.display = 'block';

        // 显示统计信息
        const finalStats = document.getElementById('final-stats');
        const giveUpText = stats.isGiveUp ? ' (已放弃)' : '';
        finalStats.innerHTML = `
            <p>正确答案: <strong>${stats.found}/${stats.total}</strong>${giveUpText}</p>
            <p>准确率: <strong>${stats.accuracy}%</strong></p>
            <p>用时: <strong>${this.formatTime(stats.timeElapsed)}</strong></p>
        `;

        // 显示未答出的答案
        const missedContainer = document.getElementById('missed-answers');
        missedContainer.innerHTML = '';
        
        if (missedAnswers.length > 0) {
            missedAnswers.forEach(answer => {
                const item = document.createElement('div');
                item.className = 'missed-answer';
                item.textContent = answer.content;
                missedContainer.appendChild(item);
            });
        } else {
            missedContainer.innerHTML = '<p style="color: #28a745; font-weight: bold;">恭喜!全部答对!</p>';
        }
    }

    /**
     * 格式化时间
     */
    static formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}分${secs}秒`;
    }
}
