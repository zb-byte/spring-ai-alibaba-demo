#!/bin/bash

echo "=== A2A gRPC Client Demo 启动脚本 ==="
echo "基于 A2A Java SDK v0.3.3.Final"
echo ""

# 检查 Java 版本
echo "🔍 检查 Java 环境..."
java -version
echo ""

# 检查 Maven 版本
echo "🔍 检查 Maven 环境..."
mvn -version
echo ""

# 编译项目
echo "🔨 编译项目..."
mvn clean compile
if [ $? -ne 0 ]; then
    echo "❌ 编译失败，请检查代码"
    exit 1
fi
echo ""

# 启动应用
echo "🚀 启动 A2A gRPC Client Demo..."
echo "📱 Web 界面: http://localhost:7001/"
echo "🔗 API 端点: http://localhost:7001/api/"
echo ""
echo "💡 确保 A2A Server 正在运行:"
echo "   cd ../a2a-server-module-grpc && ./start-server.sh"
echo ""

mvn spring-boot:run