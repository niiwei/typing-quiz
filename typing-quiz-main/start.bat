@echo off

:: 检查是否存在 .env 文件并加载
if exist .env (
    echo 正在加载环境变量...
    for /f "usebackq tokens=1,2 delims==" %%a in (.env) do (
        set %%a=%%b
    )
) else (
    echo 警告: 未找到 .env 文件，使用默认配置
    echo 请复制 .env.example 为 .env 并填写真实配置
)

:: 关闭之前可能占用端口的 Java 进程
echo 正在确保清理所有残留的 java.exe 进程...
taskkill /F /IM java.exe /T 2>nul
if %errorlevel% == 0 (
    echo 已关闭之前的 Java 进程
    timeout /t 2 /nobreak >nul
) else (
    echo 没有发现运行中的 Java 进程
)
echo.

:: 检查 MySQL_HOST 是否已设置
if "%MYSQL_HOST%"=="" (
    set MYSQL_HOST=localhost
    echo 警告: MYSQL_HOST 未设置，使用默认值 localhost
)

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
