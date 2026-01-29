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
        // 强制使用网格布局（避免残留的 display:block 导致单列）
        grid.style.display = 'grid';
        grid.style.gridTemplateColumns = '';
        grid.style.gap = '';
        grid.innerHTML = '';

        answers.forEach(answer => {
            const item = document.createElement('div');
            item.className = 'answer-item not-found';
            item.id = `answer-${answer.id}`;
            item.dataset.content = answer.content;
            item.dataset.comment = answer.comment || '';
            
            // 构建显示内容（答案 + 注释）
            let displayText = answer.content;
            if (answer.comment) {
                displayText = `<span class="answer-content">${answer.content}</span><span class="answer-comment">#${answer.comment}</span>`;
            }
            
            if (foundAnswers.has(answer.id)) {
                item.classList.remove('not-found');
                item.classList.add('found');
                item.innerHTML = displayText;
            } else {
                item.textContent = '?';
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
            
            item.classList.remove('not-found');
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
                item.classList.remove('not-found');
                item.classList.add('missed');
                
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
        
        // 填空题：保留题目区域显示（不隐藏 fill-blank-section）
        // 打字题：保留答案网格（用于显示放弃后的红/绿状态）
        const answersGrid = document.getElementById('answers-grid');
        if (answersGrid) {
            if (stats && stats.quizType === 'FILL_BLANK') {
                answersGrid.style.display = 'none';
            } else {
                // 强制显示打字题答案网格（避免残留的 display:none）
                answersGrid.style.display = 'grid';
            }
        }
        
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
                if (answer.comment) {
                    item.innerHTML = `<span class="answer-content">${answer.content}</span><span class="answer-comment">#${answer.comment}</span>`;
                } else {
                    item.textContent = answer.content;
                }
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
