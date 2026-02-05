document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');
    const submitBtn = form.querySelector('.submit-btn');

    // Check if already logged in
    if (Auth.isLoggedIn()) {
        window.location.href = 'home.html';
        return;
    }

    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        if (!username || !password) {
            showError('请填写用户名和密码');
            return;
        }

        submitBtn.classList.add('loading');
        hideError();

        try {
            const response = await api.post('/api/auth/login', {
                username: username,
                password: password
            });

            if (response.success) {
                Auth.saveToken(response.token);
                Auth.saveUser(response.user);
                window.location.href = 'home.html';
            } else {
                showError(response.message || '登录失败，请检查用户名和密码');
            }
        } catch (error) {
            console.error('Login error:', error);
            showError('登录失败，请检查用户名和密码');
        } finally {
            submitBtn.classList.remove('loading');
        }
    });

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.add('show');
    }

    function hideError() {
        errorMessage.classList.remove('show');
    }
});
