#!/bin/bash

echo "========================================"
echo "正在检测环境..."
echo "========================================"

# 检测Java
echo ""
echo "[1/2] 检测 Java JDK..."
if ! command -v java &> /dev/null; then
    echo "[错误] 未检测到Java JDK"
    echo ""
    echo "请从以下地址下载并安装Java 11或更高版本:"
    echo "https://adoptium.net/"
    echo ""
    exit 1
fi
echo "[成功] Java JDK 已安装"
java -version

# 检测Maven
echo ""
echo "[2/2] 检测 Maven..."
if ! command -v mvn &> /dev/null; then
    echo "[警告] 未检测到Maven"
    echo "[信息] 将使用Maven Wrapper (mvnw)"
else
    echo "[成功] Maven 已安装"
    mvn -version
fi

echo ""
echo "========================================"
echo "环境检测完成!"
echo "========================================"
echo ""
echo "正在安装依赖..."
echo ""

# 使用Maven Wrapper或系统Maven
if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    ./mvnw clean install -DskipTests
else
    mvn clean install -DskipTests
fi

if [ $? -ne 0 ]; then
    echo ""
    echo "[错误] 项目构建失败"
    exit 1
fi

echo ""
echo "========================================"
echo "[成功] 项目构建完成!"
echo "========================================"
echo ""
echo "运行以下命令启动应用:"
echo "  ./start.sh"
echo ""
echo "或手动运行:"
if [ -f "./mvnw" ]; then
    echo "  ./mvnw spring-boot:run"
else
    echo "  mvn spring-boot:run"
fi
echo ""
