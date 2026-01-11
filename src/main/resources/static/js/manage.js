/**
 * 测验管理页面脚本
 */

const API_BASE = '/api';
let currentQuizId = null;

// 页面加载时获取所有测验
document.addEventListener('DOMContentLoaded', loadQuizzes);

/**
 * 加载所有测验
 */
async function loadQuizzes() {
    try {
        const response = await fetch(`${API_BASE}/quizzes`);
        const quizzes = await response.json();
        
        const tbody = document.getElementById('quiz-list');
        
        if (quizzes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">暂无测验</td></tr>';
            return;
        }
        
        tbody.innerHTML = quizzes.map(quiz => `
            <tr>
                <td>${quiz.id}</td>
                <td>${quiz.title}</td>
                <td>${quiz.description || '-'}</td>
                <td>${quiz.totalAnswers}</td>
                <td>${quiz.timeLimit ? quiz.timeLimit + '秒' : '无限制'}</td>
                <td>${formatDate(quiz.createdAt)}</td>
                <td>
                    <div class="actions">
                        <button class="btn btn-secondary" onclick="exportQuiz(${quiz.id})">导出</button>
                        <button class="btn btn-warning" onclick="editQuiz(${quiz.id})">编辑</button>
                        <button class="btn btn-danger" onclick="deleteQuiz(${quiz.id})">删除</button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('加载测验失败:', error);
        alert('加载测验失败,请刷新重试');
    }
}

/**
 * 显示创建模态框
 */
function showCreateModal() {
    currentQuizId = null;
    document.getElementById('modal-title').textContent = '创建测验';
    document.getElementById('quiz-form').reset();
    document.getElementById('quiz-id').value = '';
    document.getElementById('quiz-modal').style.display = 'block';
}

/**
 * 编辑测验
 */
async function editQuiz(id) {
    try {
        // 获取测验详情
        const quizResponse = await fetch(`${API_BASE}/quizzes/${id}`);
        const quiz = await quizResponse.json();
        
        // 获取答案列表
        const answersResponse = await fetch(`${API_BASE}/quizzes/${id}/answers`);
        const answers = await answersResponse.json();
        
        // 填充表单
        currentQuizId = id;
        document.getElementById('modal-title').textContent = '编辑测验';
        document.getElementById('quiz-id').value = id;
        document.getElementById('quiz-title').value = quiz.title;
        document.getElementById('quiz-description').value = quiz.description || '';
        document.getElementById('quiz-time-limit').value = quiz.timeLimit || '';
        document.getElementById('quiz-answers').value = answers.map(a => a.content).join('\n');
        
        document.getElementById('quiz-modal').style.display = 'block';
    } catch (error) {
        console.error('加载测验失败:', error);
        alert('加载测验失败');
    }
}

/**
 * 保存测验
 */
async function saveQuiz() {
    const title = document.getElementById('quiz-title').value.trim();
    const description = document.getElementById('quiz-description').value.trim();
    const timeLimit = document.getElementById('quiz-time-limit').value;
    const answersText = document.getElementById('quiz-answers').value.trim();
    
    if (!title) {
        alert('请输入标题');
        return;
    }
    
    const answers = answersText.split('\n')
        .map(line => line.trim())
        .filter(line => line.length > 0);
    
    if (answers.length === 0) {
        alert('请至少输入一个答案');
        return;
    }
    
    const quizData = {
        title,
        description: description || null,
        timeLimit: timeLimit ? parseInt(timeLimit) : null,
        answers
    };
    
    try {
        let response;
        if (currentQuizId) {
            // 更新
            response = await fetch(`${API_BASE}/quizzes/${currentQuizId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(quizData)
            });
        } else {
            // 创建
            response = await fetch(`${API_BASE}/quizzes`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(quizData)
            });
        }
        
        if (response.ok) {
            alert(currentQuizId ? '更新成功!' : '创建成功!');
            closeModal();
            loadQuizzes();
        } else {
            alert('保存失败');
        }
    } catch (error) {
        console.error('保存失败:', error);
        alert('保存失败');
    }
}

/**
 * 删除测验
 */
async function deleteQuiz(id) {
    if (!confirm('确定要删除这个测验吗?此操作不可恢复!')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/quizzes/${id}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            alert('删除成功!');
            loadQuizzes();
        } else {
            alert('删除失败');
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('删除失败');
    }
}

/**
 * 关闭模态框
 */
function closeModal() {
    document.getElementById('quiz-modal').style.display = 'none';
}

/**
 * 格式化日期
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN');
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('quiz-modal');
    if (event.target === modal) {
        closeModal();
    }
}

/**
 * 导出单个测验
 */
async function exportQuiz(id) {
    try {
        const response = await fetch(`${API_BASE}/import-export/quiz/${id}/export`);
        const data = await response.json();
        
        // 创建下载链接
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `quiz_${id}_${data.title}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败');
    }
}

/**
 * 导出所有测验
 */
async function exportAllQuizzes() {
    try {
        const response = await fetch(`${API_BASE}/import-export/quizzes/export`);
        const data = await response.json();
        
        if (data.length === 0) {
            alert('没有可导出的测验');
            return;
        }
        
        // 创建下载链接
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `all_quizzes_${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        
        alert(`成功导出 ${data.length} 个测验`);
    } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败');
    }
}

/**
 * 显示导入模态框
 */
function showImportModal() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = handleFileImport;
    input.click();
}

/**
 * 处理文件导入
 */
async function handleFileImport(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    try {
        const text = await file.text();
        const data = JSON.parse(text);
        
        // 判断是单个测验还是多个测验
        const isArray = Array.isArray(data);
        const quizzes = isArray ? data : [data];
        
        // 验证数据格式
        for (const quiz of quizzes) {
            if (!quiz.title || !quiz.answers || !Array.isArray(quiz.answers)) {
                alert('文件格式不正确,请确保包含title和answers字段');
                return;
            }
        }
        
        // 导入
        const response = await fetch(`${API_BASE}/import-export/quizzes/import`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(quizzes)
        });
        
        const result = await response.json();
        
        // 显示结果
        let message = `导入完成!\n成功: ${result.successCount} 个\n失败: ${result.failureCount} 个`;
        
        if (result.failures.length > 0) {
            message += '\n\n失败的测验:\n';
            result.failures.forEach(f => {
                message += `- ${f.title}: ${f.error}\n`;
            });
        }
        
        alert(message);
        loadQuizzes();
    } catch (error) {
        console.error('导入失败:', error);
        alert('导入失败: ' + error.message);
    }
}
