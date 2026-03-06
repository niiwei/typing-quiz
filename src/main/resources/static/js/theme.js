/**
 * theme.js - 全局主题管理脚本
 * 负责加载用户设置并应用暗黑模式到所有页面
 */
(function() {
    // 立即执行，尽快应用主题避免闪烁
    function applyTheme() {
        try {
            const saved = localStorage.getItem('typingquiz_settings');
            if (saved) {
                const settings = JSON.parse(saved);
                if (settings.darkMode) {
                    document.documentElement.classList.add('dark-mode');
                } else {
                    document.documentElement.classList.remove('dark-mode');
                }
            }
        } catch (e) {
            console.error('应用主题失败:', e);
        }
    }

    // 立即应用
    applyTheme();

    // DOM加载完成后再次应用（确保生效）
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applyTheme);
    }
})();
