@echo off
echo ========================================
echo 正在启动 Typing Quiz 应用...
echo ========================================
echo.
echo 应用地址: http://localhost:8080
echo.
echo 访问页面:
echo   首页:     http://localhost:8080/home.html
echo   测验列表: http://localhost:8080/quizzes.html
echo   测验管理: http://localhost:8080/manage.html
echo   数据库:   http://localhost:8080/h2-console
echo.
echo 按 Ctrl+C 停止应用
echo ========================================
echo.

:: 在后台启动浏览器打开任务
start /B cmd /c "timeout /t 10 /nobreak >nul && start http://localhost:8080/home.html"

:: 启动应用(前台运行,显示日志)
if exist mvnw.cmd (
    call mvnw.cmd spring-boot:run
) else (
    call mvn spring-boot:run
)
