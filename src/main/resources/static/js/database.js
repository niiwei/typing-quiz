// 数据库管理页面JavaScript

// 页面加载时获取统计信息和初始列表
document.addEventListener('DOMContentLoaded', () => {
    loadStats();
    loadInitialQuizList();
    loadInitialAnswerList();
});

// 防抖定时器
let quizSearchTimer = null;
let answerSearchTimer = null;

// 加载数据库统计信息
async function loadStats() {
    try {
        const response = await fetch('/api/database/stats');
        const stats = await response.json();
        
        document.getElementById('total-quizzes').textContent = stats.totalQuizzes;
        document.getElementById('total-answers').textContent = stats.totalAnswers;
    } catch (error) {
        console.error('加载统计信息失败:', error);
    }
}

// 切换标签页
function switchTab(tab) {
    // 更新标签按钮状态
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
    
    // 更新内容显示
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    document.getElementById(tab + '-tab').classList.add('active');
}

// 加载初始测验列表
async function loadInitialQuizList() {
    const resultsDiv = document.getElementById('quiz-results');
    resultsDiv.innerHTML = '<div class="no-results">加载中...</div>';
    
    try {
        const response = await fetch('/api/quizzes');
        const quizzes = await response.json();
        
        if (quizzes.length === 0) {
            resultsDiv.innerHTML = '<div class="no-results">暂无测验</div>';
            return;
        }
        
        displayQuizList(quizzes, resultsDiv);
    } catch (error) {
        console.error('加载测验列表失败:', error);
        resultsDiv.innerHTML = '<div class="no-results">加载失败</div>';
    }
}

// 加载初始答案列表
async function loadInitialAnswerList() {
    const resultsDiv = document.getElementById('answer-results');
    resultsDiv.innerHTML = '<div class="no-results">加载中...</div>';
    
    try {
        // 获取所有测验的所有答案
        const quizzesResponse = await fetch('/api/quizzes');
        const quizzes = await quizzesResponse.json();
        
        let allAnswers = [];
        for (const quiz of quizzes) {
            const answersResponse = await fetch(`/api/database/quiz/${quiz.id}/answers`);
            const answers = await answersResponse.json();
            
            answers.forEach(answer => {
                allAnswers.push({
                    answerId: answer.id,
                    answerContent: answer.content,
                    quizId: quiz.id,
                    quizTitle: quiz.title
                });
            });
        }
        
        if (allAnswers.length === 0) {
            resultsDiv.innerHTML = '<div class="no-results">暂无答案</div>';
            return;
        }
        
        displayAnswerList(allAnswers, resultsDiv);
    } catch (error) {
        console.error('加载答案列表失败:', error);
        resultsDiv.innerHTML = '<div class="no-results">加载失败</div>';
    }
}

// 实时搜索测验
function searchQuizRealtime() {
    clearTimeout(quizSearchTimer);
    
    const input = document.getElementById('quiz-search-input').value.trim();
    const resultsDiv = document.getElementById('quiz-results');
    
    if (!input) {
        // 如果输入为空,显示所有测验
        loadInitialQuizList();
        return;
    }
    
    // 防抖:300ms后执行搜索
    quizSearchTimer = setTimeout(() => {
        searchQuiz(input, resultsDiv);
    }, 300);
}

// 实时搜索答案
function searchAnswerRealtime() {
    clearTimeout(answerSearchTimer);
    
    const input = document.getElementById('answer-search-input').value.trim();
    const resultsDiv = document.getElementById('answer-results');
    
    if (!input) {
        // 如果输入为空,显示所有答案
        loadInitialAnswerList();
        return;
    }
    
    // 防抖:300ms后执行搜索
    answerSearchTimer = setTimeout(() => {
        searchAnswer(input, resultsDiv);
    }, 300);
}

// 按测验检索
async function searchQuiz(input, resultsDiv) {
    resultsDiv.innerHTML = '<div class="no-results">搜索中...</div>';
    
    try {
        // 判断是ID还是名称
        const isId = /^\d+$/.test(input);
        
        if (isId) {
            // 按ID搜索
            await searchQuizById(input, resultsDiv);
        } else {
            // 按名称搜索
            await searchQuizByName(input, resultsDiv);
        }
    } catch (error) {
        console.error('搜索失败:', error);
        resultsDiv.innerHTML = '<div class="no-results">搜索失败,请重试</div>';
    }
}

// 按测验ID搜索
async function searchQuizById(id, resultsDiv) {
    try {
        // 获取测验信息
        const quizResponse = await fetch(`/api/quizzes/${id}`);
        if (!quizResponse.ok) {
            resultsDiv.innerHTML = '<div class="no-results">未找到该测验</div>';
            return;
        }
        const quiz = await quizResponse.json();
        
        // 获取测验的所有答案
        const answersResponse = await fetch(`/api/database/quiz/${id}/answers`);
        const answers = await answersResponse.json();
        
        displayQuizWithAnswers(quiz, answers, resultsDiv);
    } catch (error) {
        throw error;
    }
}

// 按测验名称搜索
async function searchQuizByName(name, resultsDiv) {
    try {
        const response = await fetch(`/api/database/quiz/search?name=${encodeURIComponent(name)}`);
        const quizzes = await response.json();
        
        if (quizzes.length === 0) {
            resultsDiv.innerHTML = '<div class="no-results">未找到匹配的测验</div>';
            return;
        }
        
        displayQuizList(quizzes, resultsDiv);
    } catch (error) {
        throw error;
    }
}

// 显示测验及其答案
function displayQuizWithAnswers(quiz, answers, container) {
    let html = `
        <h4>测验信息</h4>
        <table class="result-table">
            <tr>
                <th>ID</th>
                <th>标题</th>
                <th>描述</th>
                <th>时间限制</th>
                <th>答案数</th>
            </tr>
            <tr>
                <td>${quiz.id}</td>
                <td>${quiz.title}</td>
                <td>${quiz.description || '无'}</td>
                <td>${quiz.timeLimit ? quiz.timeLimit + '秒' : '无限制'}</td>
                <td><span class="answer-count">${answers.length}</span></td>
            </tr>
        </table>
        
        <h4 style="margin-top: 30px;">答案列表</h4>
    `;
    
    if (answers.length === 0) {
        html += '<div class="no-results">该测验暂无答案</div>';
    } else {
        html += `
            <table class="result-table">
                <thead>
                    <tr>
                        <th>答案ID</th>
                        <th>答案内容</th>
                    </tr>
                </thead>
                <tbody>
        `;
        
        answers.forEach(answer => {
            html += `
                <tr>
                    <td>${answer.id}</td>
                    <td>${answer.content}</td>
                </tr>
            `;
        });
        
        html += `
                </tbody>
            </table>
        `;
    }
    
    container.innerHTML = html;
}

// 显示测验列表
function displayQuizList(quizzes, container) {
    let html = `
        <h4>找到 ${quizzes.length} 个测验</h4>
        <table class="result-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>标题</th>
                    <th>描述</th>
                    <th>答案数</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
    `;
    
    quizzes.forEach(quiz => {
        html += `
            <tr>
                <td>${quiz.id}</td>
                <td>${quiz.title}</td>
                <td>${quiz.description || '无'}</td>
                <td><span class="answer-count">${quiz.totalAnswers}</span></td>
                <td>
                    <a href="#" class="quiz-link" onclick="viewQuizDetails(${quiz.id}); return false;">
                        查看答案
                    </a>
                </td>
            </tr>
        `;
    });
    
    html += `
            </tbody>
        </table>
    `;
    
    container.innerHTML = html;
}

// 查看测验详情
async function viewQuizDetails(quizId) {
    const resultsDiv = document.getElementById('quiz-results');
    document.getElementById('quiz-search-input').value = quizId;
    await searchQuizById(quizId, resultsDiv);
}

// 按答案检索
async function searchAnswer(input, resultsDiv) {
    resultsDiv.innerHTML = '<div class="no-results">搜索中...</div>';
    
    try {
        // 判断是ID还是内容
        const isId = /^\d+$/.test(input);
        
        if (isId) {
            // 按ID搜索
            await searchAnswerById(input, resultsDiv);
        } else {
            // 按内容搜索
            await searchAnswerByContent(input, resultsDiv);
        }
    } catch (error) {
        console.error('搜索失败:', error);
        resultsDiv.innerHTML = '<div class="no-results">搜索失败,请重试</div>';
    }
}

// 按答案ID搜索
async function searchAnswerById(id, resultsDiv) {
    try {
        const response = await fetch(`/api/database/answer/${id}/quiz`);
        if (!response.ok) {
            resultsDiv.innerHTML = '<div class="no-results">未找到该答案</div>';
            return;
        }
        
        const quiz = await response.json();
        
        // 获取该答案的详细信息
        const answersResponse = await fetch(`/api/database/quiz/${quiz.id}/answers`);
        const answers = await answersResponse.json();
        const answer = answers.find(a => a.id == id);
        
        displayAnswerWithQuiz(answer, quiz, resultsDiv);
    } catch (error) {
        throw error;
    }
}

// 按答案内容搜索
async function searchAnswerByContent(content, resultsDiv) {
    try {
        const response = await fetch(`/api/database/answer/search?content=${encodeURIComponent(content)}`);
        const answers = await response.json();
        
        if (answers.length === 0) {
            resultsDiv.innerHTML = '<div class="no-results">未找到匹配的答案</div>';
            return;
        }
        
        displayAnswerList(answers, resultsDiv);
    } catch (error) {
        throw error;
    }
}

// 显示答案及其所属测验
function displayAnswerWithQuiz(answer, quiz, container) {
    const html = `
        <h4>答案信息</h4>
        <table class="result-table">
            <tr>
                <th>答案ID</th>
                <th>答案内容</th>
            </tr>
            <tr>
                <td>${answer.id}</td>
                <td>${answer.content}</td>
            </tr>
        </table>
        
        <h4 style="margin-top: 30px;">所属测验</h4>
        <table class="result-table">
            <tr>
                <th>测验ID</th>
                <th>测验标题</th>
                <th>描述</th>
                <th>答案总数</th>
            </tr>
            <tr>
                <td>${quiz.id}</td>
                <td>${quiz.title}</td>
                <td>${quiz.description || '无'}</td>
                <td><span class="answer-count">${quiz.totalAnswers}</span></td>
            </tr>
        </table>
    `;
    
    container.innerHTML = html;
}

// 显示答案列表
function displayAnswerList(answers, container) {
    let html = `
        <h4>找到 ${answers.length} 个答案</h4>
        <table class="result-table">
            <thead>
                <tr>
                    <th>答案ID</th>
                    <th>答案内容</th>
                    <th>所属测验ID</th>
                    <th>所属测验标题</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
    `;
    
    answers.forEach(answer => {
        html += `
            <tr>
                <td>${answer.answerId}</td>
                <td>${answer.answerContent}</td>
                <td>${answer.quizId}</td>
                <td>${answer.quizTitle}</td>
                <td>
                    <a href="#" class="quiz-link" onclick="viewAnswerDetails(${answer.answerId}); return false;">
                        查看详情
                    </a>
                </td>
            </tr>
        `;
    });
    
    html += `
            </tbody>
        </table>
    `;
    
    container.innerHTML = html;
}

// 查看答案详情
async function viewAnswerDetails(answerId) {
    // 切换到答案标签页
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-btn')[1].classList.add('active');
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    document.getElementById('answer-tab').classList.add('active');
    
    const resultsDiv = document.getElementById('answer-results');
    document.getElementById('answer-search-input').value = answerId;
    await searchAnswerById(answerId, resultsDiv);
}
