document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('registerForm');
    const errorMessage = document.getElementById('errorMessage');
    const submitBtn = form.querySelector('.submit-btn');
    const passwordInput = document.getElementById('password');
    const strengthBar = document.getElementById('strengthBar');

    // Check if already logged in
    if (Auth.isLoggedIn()) {
        window.location.href = 'home.html';
        return;
    }

    // Password strength indicator
    passwordInput.addEventListener('input', function() {
        const password = this.value;
        let strength = 0;

        if (password.length >= 8) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[^A-Za-z0-9]/.test(password)) strength++;

        strengthBar.className = 'password-strength-bar';
        if (strength <= 1) {
            strengthBar.classList.add('strength-weak');
        } else if (strength <= 2) {
            strengthBar.classList.add('strength-medium');
        } else {
            strengthBar.classList.add('strength-strong');
        }
    });

    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        const username = document.getElementById('username').value.trim();
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (!username || !email || !password || !confirmPassword) {
            showError('请填写所有字段');
            return;
        }

        if (password !== confirmPassword) {
            showError('两次输入的密码不一致');
            return;
        }

        submitBtn.classList.add('loading');
        hideError();

        try {
            const response = await api.post('/auth/register', {
                username: username,
                email: email,
                password: password
            });

            if (response.success) {
                Auth.saveToken(response.token);
                Auth.saveUser(response.user);
                window.location.href = 'home.html';
            } else {
                showError(response.message || '注册失败');
            }
        } catch (error) {
            console.error('Register error:', error);
            showError(error.message || '注册失败，请稍后重试');
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
