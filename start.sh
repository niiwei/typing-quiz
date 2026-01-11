#!/bin/bash

echo "========================================"
echo "正在启动 Typing Quiz 应用..."
echo "========================================"
echo ""
echo "应用将在以下地址运行:"
echo "  http://localhost:8080"
echo ""
echo "H2 数据库控制台:"
echo "  http://localhost:8080/h2-console"
echo ""
echo "按 Ctrl+C 停止应用"
echo ""
echo "========================================"
echo ""

if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    ./mvnw spring-boot:run
else
    mvn spring-boot:run
fi
