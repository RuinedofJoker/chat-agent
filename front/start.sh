#!/bin/bash

echo "========================================"
echo "AI Agent 聊天前端 - 启动脚本"
echo "========================================"
echo ""

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "❌ 未检测到Node.js，请先安装Node.js"
    exit 1
fi

echo "✅ Node.js版本: $(node -v)"

# 检查npm
if ! command -v npm &> /dev/null; then
    echo "❌ 未检测到npm"
    exit 1
fi

echo "✅ npm版本: $(npm -v)"
echo ""

# 检查依赖
if [ ! -d "node_modules" ]; then
    echo "📦 正在安装依赖..."
    npm install
    echo ""
fi

# 启动开发服务器
echo "🚀 启动开发服务器..."
echo "前端地址: http://localhost:3003"
echo "按 Ctrl+C 停止服务"
echo ""

npm run dev
