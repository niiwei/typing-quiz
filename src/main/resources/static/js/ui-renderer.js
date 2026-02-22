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
     * 显示最终结果 - 内嵌式布局（保留题目区域）
     */
    static showResults(stats, missedAnswers) {
        // 1. 隐藏输入区域和进度条
        const inputSection = document.getElementById('input-section');
        if (inputSection) inputSection.style.display = 'none';
        
        const progressBar = document.querySelector('.progress-bar');
        if (progressBar) progressBar.style.display = 'none';

        // 2. 保持答案网格/填空题显示，标记最终状态
        if (stats.quizType === 'FILL_BLANK') {
            // 填空题：保持原文显示
            const fillBlankSection = document.getElementById('fill-blank-section');
            if (fillBlankSection) fillBlankSection.style.display = 'block';
        } else {
            // 打字题：保持答案网格，标记未答出项
            const answersGrid = document.getElementById('answers-grid');
            if (answersGrid) {
                answersGrid.style.display = 'grid';
                // 标记所有未答出的答案为红色
                const allItems = answersGrid.querySelectorAll('.answer-item');
                allItems.forEach(item => {
                    if (!item.classList.contains('found')) {
                        item.classList.add('missed');
                        // 显示答案内容
                        const content = item.dataset.content;
                        const comment = item.dataset.comment;
                        if (comment) {
                            item.innerHTML = `<span class="answer-content">${content}</span><span class="answer-comment">#${comment}</span>`;
                        } else {
                            item.textContent = content;
                        }
                    }
                });
            }
        }
        
        // 3. 显示结算面板（内嵌在题目下方）
        const resultsPanel = document.getElementById('results-panel');
        if (resultsPanel) {
            resultsPanel.style.display = 'block';
            // 滚动到结算区域
            setTimeout(() => {
                resultsPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }, 100);
        }

        // 4. 更新统计卡片
        const accuracyEl = document.getElementById('final-accuracy');
        const scoreEl = document.getElementById('final-score');
        const timeEl = document.getElementById('final-time');
        
        if (accuracyEl) accuracyEl.textContent = `${stats.accuracy}%`;
        if (scoreEl) scoreEl.textContent = `${stats.found}/${stats.total}`;
        if (timeEl) timeEl.textContent = this.formatTime(stats.timeElapsed);

        // 5. 显示未答出列表或全部答对提示
        const missedSection = document.getElementById('missed-section');
        const perfectSection = document.getElementById('perfect-section');
        const missedContainer = document.getElementById('missed-answers');
        
        if (missedContainer) {
            missedContainer.innerHTML = '';
        }
        
        if (missedAnswers.length > 0) {
            // 有未答出项：显示列表
            if (missedSection) missedSection.style.display = 'block';
            if (perfectSection) perfectSection.style.display = 'none';
            
            missedAnswers.forEach(answer => {
                const item = document.createElement('div');
                item.className = 'missed-item';
                
                // 构建内容：答案 + 注释
                let contentHtml = `<span class="missed-content">${answer.content}</span>`;
                if (answer.comment) {
                    contentHtml += `<span class="missed-comment">${answer.comment}</span>`;
                }
                item.innerHTML = contentHtml;
                missedContainer.appendChild(item);
            });
        } else {
            // 全部答对：显示提示
            if (missedSection) missedSection.style.display = 'none';
            if (perfectSection) perfectSection.style.display = 'flex';
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
