/**
 * 主入口文件
 * 负责初始化应用
 */

// 全局控制器实例
let quizController = null;

// 从URL参数获取quizId
function getQuizIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id') || '1'; // 默认使用ID为1的测验
}

// 全局放弃函数
function giveUp() {
    if (quizController) {
        quizController.giveUp();
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const quizId = getQuizIdFromUrl();
        quizController = new QuizController(quizId);
        window.quizController = quizController; // 暴露到全局以便提示功能访问
        await quizController.init();
    } catch (error) {
        console.error('应用初始化失败:', error);
        
        // 显示错误信息
        document.getElementById('quiz-title').textContent = '加载失败';
        document.getElementById('quiz-description').textContent = 
            '无法加载测验,请检查测验ID是否正确或刷新页面重试';
    }
});
