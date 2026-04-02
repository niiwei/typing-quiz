# 加载 .env 文件中的环境变量
$envFile = ".env"
if (Test-Path $envFile) {
    Write-Host "正在加载环境变量..." -ForegroundColor Green
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^([^#][^=]*)=(.*)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
} else {
    Write-Host "警告: 未找到 .env 文件，使用默认配置" -ForegroundColor Yellow
    Write-Host "请复制 .env.example 为 .env 并填写真实配置"
}

# 检查必要的环境变量
if (-not $env:MYSQL_HOST) {
    $env:MYSQL_HOST = "localhost"
    Write-Host "警告: MYSQL_HOST 未设置，使用默认值 localhost" -ForegroundColor Yellow
}

echo "========================================"
echo "正在启动 Typing Quiz 应用..."
echo "MySQL 主机: $($env:MYSQL_HOST)"
echo "========================================"
echo ""
echo "应用地址: http://localhost:8080"

# 检查并释放端口
$portProcess = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portProcess) {
    echo "正在释放 8080 端口 (PID: $($portProcess.OwningProcess))..."
    Stop-Process -Id $portProcess.OwningProcess -Force -ErrorAction SilentlyContinue
}

# 确保所有残留的 java.exe 进程被杀掉，防止多实例冲突
echo "正在确保清理所有残留的 java.exe 进程..."
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# 启动应用
if (Test-Path ".\mvnw.cmd") {
    .\mvnw.cmd spring-boot:run
} else {
    mvn spring-boot:run
}
