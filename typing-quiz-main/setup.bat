@echo off
echo ========================================
echo 正在检测环境...
echo ========================================

:: 检测Java
echo.
echo [1/2] 检测 Java JDK...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Java JDK
    echo.
    echo 请从以下地址下载并安装Java 11或更高版本:
    echo https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo [成功] Java JDK 已安装
java -version

:: 检测Maven
echo.
echo [2/2] 检测 Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未检测到Maven
    echo [信息] 将使用Maven Wrapper (mvnw.cmd)
) else (
    echo [成功] Maven 已安装
    mvn -version
)

echo.
echo ========================================
echo 环境检测完成!
echo ========================================
echo.
echo 正在安装依赖...
echo.

if exist mvnw.cmd (
    call mvnw.cmd clean install -DskipTests
) else (
    call mvn clean install -DskipTests
)

if %errorlevel% neq 0 (
    echo.
    echo [错误] 项目构建失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo [成功] 项目构建完成!
echo ========================================
echo.
echo 运行以下命令启动应用:
echo   start.bat
echo.
echo 或手动运行:
if exist mvnw.cmd (
    echo   mvnw.cmd spring-boot:run
) else (
    echo   mvn spring-boot:run
)
echo.
pause
