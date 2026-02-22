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
            item.className = 'answer-item'; // 使用新版 CSS 类名
            item.id = `answer-${answer.id}`;
            item.dataset.content = answer.content;
            item.dataset.comment = answer.comment || '';
            
            if (foundAnswers.has(answer.id)) {
                item.classList.add('found');
                let displayText = answer.content;
                if (answer.comment) {
                    displayText = `<span class="answer-content">${answer.content}</span><span class="answer-comment">#${answer.comment}</span>`;
                }
                item.innerHTML = displayText;
            } else {
                item.textContent = '•'; // 使用新版占位符
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
            const content = item.dataset.content;
            const comment = item.dataset.comment;
            
            item.classList.add('found');
            
            // 构建显示内容
            if (comment) {
                item.innerHTML = `<span class="answer-content">${content}</span><span class="answer-comment">#${comment}</span>`;
            } else {
                item.textContent = content;
            }
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
                item.classList.add('missed'); // 使用新版红色样式
                
                // 构建显示内容
                if (answer.comment) {
                    item.innerHTML = `<span class="answer-content">${answer.content}</span><span class="answer-comment">#${answer.comment}</span>`;
                } else {
                    item.textContent = answer.content;
                }
            }
        });
    }

    /**
     * 显示最终结果
     */
    static showResults(stats, missedAnswers) {
        // 隐藏输入区域
        document.getElementById('input-section').style.display = 'none';
        
        // 隐藏进度条
        const progressBar = document.querySelector('.progress-bar');
        if (progressBar) progressBar.style.display = 'none';

        const answersGrid = document.getElementById('answers-grid');
        if (answersGrid) {
            if (stats && stats.quizType === 'FILL_BLANK') {
                answersGrid.style.display = 'none';
            } else {
                answersGrid.style.display = 'grid';
            }
        }
        
        // 显示结果面板
        const resultsPanel = document.getElementById('results-panel');
        resultsPanel.style.display = 'block';

        // 更新结果统计
        document.getElementById('final-accuracy').textContent = `${stats.accuracy}%`;
        document.getElementById('final-score').textContent = stats.found;
        document.getElementById('final-time').textContent = this.formatTime(stats.timeElapsed);

        // 显示未答出的答案
        const missedContainer = document.getElementById('missed-answers');
        missedContainer.innerHTML = '';
        
        if (missedAnswers.length > 0) {
            missedAnswers.forEach(answer => {
                const item = document.createElement('div');
                item.className = 'answer-item missed';
                if (answer.comment) {
                    item.innerHTML = `<span class="answer-content">${answer.content}</span><span class="answer-comment">#${answer.comment}</span>`;
                } else {
                    item.textContent = answer.content;
                }
                missedContainer.appendChild(item);
            });
        } else {
            missedContainer.innerHTML = '<p style="color: var(--success); font-weight: bold; width: 100%; text-align: center;">恭喜! 全部答对!</p>';
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
