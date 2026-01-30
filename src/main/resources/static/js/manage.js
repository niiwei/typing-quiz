/**
 * 测验管理页面脚本
 */

const API_BASE = '/api';
let currentQuizId = null;
let currentGroupId = null;

// 页面加载时获取所有测验和分组
document.addEventListener('DOMContentLoaded', () => {
    loadQuizzes();
    loadGroups();
});

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
 * 加载所有分组
 */
async function loadGroups() {
    try {
        const response = await fetch(`${API_BASE}/groups`);
        const groups = await response.json();
        
        const tbody = document.getElementById('group-list');
        
        if (groups.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无分组</td></tr>';
            return;
        }
        
        tbody.innerHTML = groups.map(group => `
            <tr>
                <td>${group.id}</td>
                <td>${group.name}</td>
                <td>${group.description || '-'}</td>
                <td>${group.quizIds ? group.quizIds.length : 0}</td>
                <td>${formatDate(group.createdAt)}</td>
                <td>
                    <div class="actions">
                        <button class="btn btn-warning" onclick="editGroup(${group.id})">编辑</button>
                        <button class="btn btn-danger" onclick="deleteGroup(${group.id})">删除</button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('加载分组失败:', error);
        document.getElementById('group-list').innerHTML = '<tr><td colspan="6" style="text-align: center;">加载失败</td></tr>';
    }
}

/**
 * 显示创建分组模态框
 */
async function showGroupModal() {
    currentGroupId = null;
    document.getElementById('group-modal-title').textContent = '创建分组';
    document.getElementById('group-form').reset();
    document.getElementById('group-id').value = '';
    await loadQuizCheckboxes([]);
    document.getElementById('group-modal').style.display = 'block';
}

/**
 * 编辑分组
 */
async function editGroup(id) {
    try {
        // 获取分组详情
        const groupResponse = await fetch(`${API_BASE}/groups/${id}`);
        const group = await groupResponse.json();
        
        currentGroupId = id;
        document.getElementById('group-modal-title').textContent = '编辑分组';
        document.getElementById('group-id').value = id;
        document.getElementById('group-name').value = group.name;
        document.getElementById('group-description').value = group.description || '';
        
        // 加载测验列表并勾选已关联的
        await loadQuizCheckboxes(group.quizIds || []);
        
        document.getElementById('group-modal').style.display = 'block';
    } catch (error) {
        console.error('加载分组失败:', error);
        alert('加载分组失败');
    }
}

/**
 * 加载测验复选框
 */
async function loadQuizCheckboxes(selectedIds) {
    try {
        const response = await fetch(`${API_BASE}/quizzes`);
        const quizzes = await response.json();
        
        const container = document.getElementById('quiz-checkboxes');
        
        if (quizzes.length === 0) {
            container.innerHTML = '<p style="color: #999;">暂无测验可选择</p>';
            return;
        }
        
        container.innerHTML = quizzes.map(quiz => `
            <label style="display: block; padding: 5px 0;">
                <input type="checkbox" value="${quiz.id}" ${selectedIds.includes(quiz.id) ? 'checked' : ''}>
                ${quiz.title} (${quiz.totalAnswers}个答案)
            </label>
        `).join('');
    } catch (error) {
        console.error('加载测验列表失败:', error);
        document.getElementById('quiz-checkboxes').innerHTML = '<p style="color: red;">加载失败</p>';
    }
}

/**
 * 保存分组
 */
async function saveGroup() {
    const name = document.getElementById('group-name').value.trim();
    const description = document.getElementById('group-description').value.trim();
    
    // 获取选中的测验
    const checkboxes = document.querySelectorAll('#quiz-checkboxes input[type="checkbox"]:checked');
    const quizIds = Array.from(checkboxes).map(cb => parseInt(cb.value));
    
    if (!name) {
        alert('请输入分组名称');
        return;
    }
    
    const groupData = {
        name,
        description: description || null,
        quizIds
    };
    
    try {
        let response;
        if (currentGroupId) {
            // 更新
            response = await fetch(`${API_BASE}/groups/${currentGroupId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(groupData)
            });
        } else {
            // 创建
            response = await fetch(`${API_BASE}/groups`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(groupData)
            });
        }
        
        if (response.ok) {
            alert(currentGroupId ? '更新成功!' : '创建成功!');
            closeGroupModal();
            loadGroups();
        } else {
            const error = await response.text();
            alert('保存失败: ' + error);
        }
    } catch (error) {
        console.error('保存分组失败:', error);
        alert('保存失败');
    }
}

/**
 * 删除分组
 */
async function deleteGroup(id) {
    if (!confirm('确定要删除这个分组吗?测验不会被删除。')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/groups/${id}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            alert('删除成功!');
            loadGroups();
        } else {
            alert('删除失败');
        }
    } catch (error) {
        console.error('删除分组失败:', error);
        alert('删除失败');
    }
}

/**
 * 关闭分组模态框
 */
function closeGroupModal() {
    document.getElementById('group-modal').style.display = 'none';
    currentGroupId = null;
}

/**
 * 显示创建模态框
 */
function showCreateModal() {
    currentQuizId = null;
    document.getElementById('modal-title').textContent = '创建测验';
    document.getElementById('quiz-form').reset();
    document.getElementById('quiz-id').value = '';
    document.getElementById('quiz-type').value = 'TYPING';
    toggleQuizTypeFields();
    document.getElementById('quiz-modal').style.display = 'block';
}

/**
 * 切换测验类型字段显示
 */
function toggleQuizTypeFields() {
    const quizType = document.getElementById('quiz-type').value;
    const typingSection = document.getElementById('typing-answers-section');
    const fillBlankSection = document.getElementById('fill-blank-section');
    
    if (quizType === 'FILL_BLANK') {
        typingSection.style.display = 'none';
        fillBlankSection.style.display = 'block';
    } else {
        typingSection.style.display = 'block';
        fillBlankSection.style.display = 'none';
    }
}

/**
 * 生成填空预览
 */
function generateFillBlanks() {
    const fullText = document.getElementById('fill-blank-full-text').value;
    if (!fullText) {
        document.getElementById('fill-blank-preview').innerHTML = '<span style="color: #999;">请先输入完整文本</span>';
        return;
    }

    // 使用正则表达式匹配 [答案] 格式
    const blankRegex = /\[([^\]]+)\]/g;
    const matches = [...fullText.matchAll(blankRegex)];

    if (matches.length === 0) {
        document.getElementById('fill-blank-preview').innerHTML = '<span style="color: #999;">未找到填空标记，请用[]包围答案，如: 中国的首都是[北京]</span>';
        return;
    }

    // 生成预览文本
    let previewText = fullText.replace(blankRegex, '___');
    document.getElementById('fill-blank-preview').innerHTML = previewText;
}

/**
 * 注释选中文字（用 ## 包围，用于打字题）
 */
function wrapAnswerWithComment() {
    const textarea = document.getElementById('quiz-answers');
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;

    if (start === end) {
        alert('请先选中要添加注释的文字');
        return;
    }

    const selectedText = text.substring(start, end);
    // 用 ## 包围选中文字
    const newText = text.substring(0, start) + '##' + selectedText + '##' + text.substring(end);
    textarea.value = newText;

    // 重新设置光标位置
    textarea.focus();
    textarea.selectionStart = start + 2;
    textarea.selectionEnd = end + 2;
}

/**
 * 选中文字后用[]包围（挖空功能）
 */
function wrapSelectionWithBrackets() {
    const textarea = document.getElementById('fill-blank-full-text');
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;

    if (start === end) {
        alert('请先选中要挖空的文字');
        return;
    }

    const text = textarea.value;
    const selectedText = text.substring(start, end);

    const newText = text.substring(0, start) + '[' + selectedText + ']' + text.substring(end);
    textarea.value = newText;

    // 重新设置光标位置
    textarea.focus();
    textarea.selectionStart = start + 1;
    textarea.selectionEnd = end + 1;

    // 更新预览
    generateFillBlanks();
}

/**
 * 注释选中文字（用 ## 包围）
 */
function wrapSelectionWithComment() {
    const textarea = document.getElementById('fill-blank-full-text');
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;

    if (start === end) {
        alert('请先选中要添加注释的文字');
        return;
    }

    const selectedText = text.substring(start, end);
    // 用 ## 包围选中文字
    const newText = text.substring(0, start) + '##' + selectedText + '##' + text.substring(end);
    textarea.value = newText;

    // 重新设置光标位置
    textarea.focus();
    textarea.selectionStart = start + 2;
    textarea.selectionEnd = end + 2;

    // 更新预览
    generateFillBlanks();
}

/**
 * 编辑测验
 */
async function editQuiz(id) {
    try {
        // 获取测验详情
        const quizResponse = await fetch(`${API_BASE}/quizzes/${id}`);
        const quiz = await quizResponse.json();
        
        // 填充表单
        currentQuizId = id;
        document.getElementById('modal-title').textContent = '编辑测验';
        document.getElementById('quiz-id').value = id;
        document.getElementById('quiz-title').value = quiz.title;
        document.getElementById('quiz-description').value = quiz.description || '';
        document.getElementById('quiz-time-limit').value = quiz.timeLimit || '';
        document.getElementById('quiz-type').value = quiz.quizType || 'TYPING';
        toggleQuizTypeFields();
        
        // 根据类型加载不同数据
        if (quiz.quizType === 'FILL_BLANK' && quiz.fillBlankQuiz) {
            // 填空题
            document.getElementById('fill-blank-full-text').value = quiz.fillBlankQuiz.fullText || '';
            document.getElementById('fill-blank-preview').innerHTML = quiz.fillBlankQuiz.displayText || '';
            document.getElementById('quiz-answers').value = '';
        } else {
            // 打字题
            const answersResponse = await fetch(`${API_BASE}/quizzes/${id}/answers`);
            const answers = await answersResponse.json();
            // 显示答案和注释，格式：答案##注释##
            document.getElementById('quiz-answers').value = answers.map(a => {
                if (a.comment) {
                    return `${a.content}##${a.comment}##`;
                }
                return a.content;
            }).join('\n');
            document.getElementById('fill-blank-full-text').value = '';
            document.getElementById('fill-blank-preview').innerHTML = '';
        }
        
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
    const quizType = document.getElementById('quiz-type').value;
    
    if (!title) {
        alert('请输入标题');
        return;
    }
    
    let quizData = {
        title,
        description: description || null,
        timeLimit: timeLimit ? parseInt(timeLimit) : null,
        quizType: quizType
    };
    
    if (quizType === 'FILL_BLANK') {
        // 填空题处理
        const fullText = document.getElementById('fill-blank-full-text').value.trim();
        if (!fullText) {
            alert('请输入完整文本');
            return;
        }

        // 解析填空标记 [答案##注释##]，支持 ## 语法
        const blankRegex = /\[([^\]]+)\]/g;
        const blanks = [];
        let match;
        while ((match = blankRegex.exec(fullText)) !== null) {
            // 解析答案和注释（格式: 答案##注释##）
            const fullMatch = match[1];
            const commentRegex = /^(.+?)##(.+?)##$/;
            const commentMatch = fullMatch.match(commentRegex);
            let correctAnswer;
            let comment = null;

            if (commentMatch) {
                correctAnswer = commentMatch[1].trim();
                comment = commentMatch[2].trim();
            } else {
                correctAnswer = fullMatch;
            }

            blanks.push({
                startIndex: match.index,
                endIndex: match.index + match[0].length,
                correctAnswer: correctAnswer,
                comment: comment
            });
        }

        if (blanks.length === 0) {
            alert('请用[]包围需要填空的内容，如: 中国的首都是[北京]');
            return;
        }

        // 生成显示文本（用___替换挖空部分，保留注释在原位置）
        let displayText = fullText;
        blanks.forEach(blank => {
            const placeholderHTML = '<span class="fill-blank-placeholder" data-blank-index="' + blanks.indexOf(blank) + '">___</span>';
            displayText = displayText.substring(0, blank.startIndex) + placeholderHTML + displayText.substring(blank.endIndex);
        });

        quizData.fillBlankQuiz = {
            fullText: fullText,
            displayText: displayText,
            blanks: blanks,
            blanksCount: blanks.length
        };
    } else {
        // 打字题处理
        const answersText = document.getElementById('quiz-answers').value.trim();
        const answers = answersText.split('\n')
            .map(line => line.trim())
            .filter(line => line.length > 0);
        
        if (answers.length === 0) {
            alert('请至少输入一个答案');
            return;
        }
        
        // 解析答案中的注释（格式: 答案##注释##）
        const parsedAnswers = answers.map(answer => {
            const commentRegex = /^(.+?)##(.+?)##$/;
            const commentMatch = answer.match(commentRegex);
            if (commentMatch) {
                return {
                    content: commentMatch[1].trim(),
                    comment: commentMatch[2].trim()
                };
            }
            return {
                content: answer,
                comment: null
            };
        });
        
        quizData.answerList = parsedAnswers;
    }
    
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
            const error = await response.text();
            alert('保存失败: ' + error);
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
    const quizModal = document.getElementById('quiz-modal');
    const groupModal = document.getElementById('group-modal');
    if (event.target === quizModal) {
        closeModal();
    }
    if (event.target === groupModal) {
        closeGroupModal();
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
