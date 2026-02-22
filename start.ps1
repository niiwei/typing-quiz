# 设置云端数据库环境变量
$env:MYSQL_HOST = "47.102.147.127"

echo "========================================"
echo "正在启动 Typing Quiz 应用 (连接云端数据库)..."
echo "========================================"
echo ""
echo "应用地址: http://localhost:8080"

# 检查并释放端口
$portProcess = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portProcess) {
    echo "正在释放 8080 端口..."
    Stop-Process -Id $portProcess.OwningProcess -Force
}

# 启动应用
if (Test-Path ".\mvnw.cmd") {
    .\mvnw.cmd spring-boot:run
} else {
    mvn spring-boot:run
}
