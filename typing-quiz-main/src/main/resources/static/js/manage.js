/**
 * 测验管理页面脚本 - Minimalist Zen 适配版
 */

const API_BASE = '/api';
let currentQuizId = null;
let currentGroupId = null;
let selectedQuizzes = new Set(); // 存储选中的测验 ID

// 获取带 token 的请求头
function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };
    const token = localStorage.getItem('typingquiz_token');
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

// 页面加载时获取所有测验和分组
document.addEventListener('DOMContentLoaded', () => {
    if (typeof Auth !== 'undefined' && !Auth.isLoggedIn()) {
        window.location.href = 'login.html';
        return;
    }
    loadQuizzes();
    loadGroups();
});

/**
 * 加载所有测验
 */
async function loadQuizzes() {
    try {
        const [quizzesRes, groupsRes] = await Promise.all([
            fetch(`${API_BASE}/quizzes`, { headers: getAuthHeaders() }),
            fetch(`${API_BASE}/groups`, { headers: getAuthHeaders() })
        ]);
        
        const quizzes = await quizzesRes.json();
        const groups = await groupsRes.json();
        
        window.allQuizzes = quizzes;
        window.allGroups = groups;

        renderQuizList(quizzes);
    } catch (error) {
        console.error('加载测验失败:', error);
        const listEl = document.getElementById('quiz-list');
        if (listEl) listEl.innerHTML = '<div style="text-align: center; color: var(--error);">加载失败</div>';
    }
}

/**
 * 加载所有分组
 */
async function loadGroups() {
    try {
        const response = await fetch(`${API_BASE}/groups`, { headers: getAuthHeaders() });
        const groups = await response.json();
        window.allGroups = groups;
        renderGroupList(groups);
    } catch (error) {
        console.error('加载分组失败:', error);
        const groupEl = document.getElementById('group-list');
        if (groupEl) groupEl.innerHTML = '<div style="text-align: center; color: var(--error);">加载失败</div>';
    }
}

/**
 * 渲染测验列表 (适配 Minimalist Zen)
 */
function renderQuizList(quizzes) {
    const container = document.getElementById('quiz-list');
    if (!container) return;
    
    if (quizzes.length === 0) {
        container.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--text-muted); font-size: 0.8rem;">暂无测验</div>';
        return;
    }
    
    container.innerHTML = quizzes.map(quiz => `
        <div class="list-item">
            <div style="display: flex; align-items: center; gap: 1rem;">
                <input type="checkbox" class="quiz-checkbox" value="${quiz.id}" ${selectedQuizzes.has(quiz.id) ? 'checked' : ''} onchange="toggleQuizSelection(${quiz.id})">
                <div class="list-info">
                    <span class="title">${quiz.title}</span>
                    <span class="sub">ID: ${quiz.id} • ${quiz.quizType === 'FILL_BLANK' ? '填空题' : '打字题'} • ${quiz.totalAnswers}个答案</span>
                </div>
            </div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-small" style="padding: 0.2rem 0.5rem; border-color: var(--border); color: var(--text-muted);" onclick="editQuiz(${quiz.id})">编辑</button>
                <button class="btn btn-small" style="padding: 0.2rem 0.5rem; border-color: var(--error); color: var(--error);" onclick="deleteQuiz(${quiz.id})">删除</button>
            </div>
        </div>
    `).join('');
    
    const actionBar = document.getElementById('bulk-actions-bar');
    if (actionBar) actionBar.style.display = 'flex';
}

/**
 * 渲染分组列表 (适配 Minimalist Zen)
 */
function renderGroupList(groups) {
    const container = document.getElementById('group-list');
    if (!container) return;
    
    if (groups.length === 0) {
        container.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--text-muted); font-size: 0.8rem;">暂无分组</div>';
        return;
    }
    
    container.innerHTML = groups.map(group => `
        <div class="list-item">
            <div class="list-info">
                <span class="title">${group.name}</span>
                <span class="sub">${group.quizIds ? group.quizIds.length : 0} 个测验 • ${group.description || '无描述'}</span>
            </div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-small" style="padding: 0.2rem 0.5rem; border-color: var(--border); color: var(--text-muted);" onclick="editGroup(${group.id})">编辑</button>
                <button class="btn btn-small" style="padding: 0.2rem 0.5rem; border-color: var(--error); color: var(--error);" onclick="deleteGroup(${group.id})">删除</button>
            </div>
        </div>
    `).join('');
}

/**
 * 批量操作逻辑
 */
function toggleQuizSelection(quizId) {
    if (selectedQuizzes.has(quizId)) {
        selectedQuizzes.delete(quizId);
    } else {
        selectedQuizzes.add(quizId);
    }
    updateSelectedCount();
}

function updateSelectedCount() {
    const count = selectedQuizzes.size;
    const el = document.getElementById('selected-count');
    if (el) el.textContent = `已选 ${count} 项`;
}

function toggleSelectAll(checkbox) {
    const checkboxes = document.querySelectorAll('.quiz-checkbox');
    checkboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        const id = parseInt(cb.value);
        if (checkbox.checked) selectedQuizzes.add(id);
        else selectedQuizzes.delete(id);
    });
    updateSelectedCount();
}

/**
 * 编辑跳转
 */
function editQuiz(id) {
    window.location.href = `create.html?id=${id}`;
}

async function editGroup(id) {
    currentGroupId = id;
    try {
        const res = await fetch(`${API_BASE}/groups/${id}`, { headers: getAuthHeaders() });
        const group = await res.json();
        const idEl = document.getElementById('group-id');
        const nameEl = document.getElementById('group-name');
        const descEl = document.getElementById('group-description');
        const titleEl = document.getElementById('group-modal-title');
        
        if (idEl) idEl.value = id;
        if (nameEl) nameEl.value = group.name;
        if (descEl) descEl.value = group.description || '';
        if (titleEl) titleEl.textContent = '编辑分组';
        showGroupModal();
    } catch (e) { alert('加载分组失败'); }
}

/**
 * 分组模态框操作
 */
function showGroupModal() {
    const modal = document.getElementById('group-modal');
    if (modal) modal.style.display = 'flex';
}

function closeGroupModal() {
    const modal = document.getElementById('group-modal');
    if (modal) modal.style.display = 'none';
    currentGroupId = null;
}

async function saveGroup() {
    const id = document.getElementById('group-id').value;
    const name = document.getElementById('group-name').value.trim();
    const description = document.getElementById('group-description').value.trim();
    
    if (!name) { alert('请输入名称'); return; }
    
    const data = { name, description };
    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/groups/${id}` : `${API_BASE}/groups`;
    
    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(data)
        });
        if (res.ok) {
            closeGroupModal();
            loadGroups();
        }
    } catch (e) { alert('保存失败'); }
}

/**
 * 删除操作
 */
async function deleteQuiz(id) {
    if (!confirm('确定删除该测验吗？')) return;
    try {
        const res = await fetch(`${API_BASE}/quizzes/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (res.ok) loadQuizzes();
    } catch (e) { alert('删除失败'); }
}

async function deleteGroup(id) {
    if (!confirm('确定删除该分组吗？（测验不会被删除）')) return;
    try {
        const res = await fetch(`${API_BASE}/groups/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
        if (res.ok) loadGroups();
    } catch (e) { alert('删除失败'); }
}

/**
 * 批量操作执行
 */
async function bulkDelete() {
    if (selectedQuizzes.size === 0) return;
    if (!confirm(`确定删除选中的 ${selectedQuizzes.size} 项吗？`)) return;
    
    const ids = Array.from(selectedQuizzes);
    for (const id of ids) {
        await fetch(`${API_BASE}/quizzes/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
    }
    selectedQuizzes.clear();
    updateSelectedCount();
    loadQuizzes();
}

function showBulkGroupModal() {
    if (selectedQuizzes.size === 0) { alert('请先选择测验'); return; }
    const select = document.getElementById('bulk-group-select');
    if (select) {
        select.innerHTML = window.allGroups.map(g => `<option value="${g.id}">${g.name}</option>`).join('');
    }
    const modal = document.getElementById('bulk-group-modal');
    if (modal) modal.style.display = 'flex';
}

function closeBulkGroupModal() {
    const modal = document.getElementById('bulk-group-modal');
    if (modal) modal.style.display = 'none';
}

async function bulkAddToGroup() {
    const select = document.getElementById('bulk-group-select');
    if (!select) return;
    const groupId = select.value;
    const ids = Array.from(selectedQuizzes);
    
    for (const quizId of ids) {
        await fetch(`${API_BASE}/groups/${groupId}/quizzes/${quizId}`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
    }
    alert('移动成功');
    closeBulkGroupModal();
    selectedQuizzes.clear();
    updateSelectedCount();
    loadQuizzes();
}

async function exportAllQuizzes() {
    try {
        const res = await fetch(`${API_BASE}/import-export/quizzes/export`, { headers: getAuthHeaders() });
        const data = await res.json();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `typing_quiz_backup_${new Date().toISOString().split('T')[0]}.json`;
        a.click();
    } catch (e) { alert('导出失败'); }
}
