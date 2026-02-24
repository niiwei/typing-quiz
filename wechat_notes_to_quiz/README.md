# 微信读书笔记 → 填空题生成器

## 使用方法

1. 安装依赖：
```bash
pip install -r requirements.txt
```

2. 配置API密钥：
```bash
# Windows
set OPENAI_API_KEY=your-api-key

# 或在代码中直接修改 API_KEY 变量
```

3. 运行：
```bash
python wechat_notes_to_quiz.py input.csv
```

## 输入输出

- **输入**：CSV文件（包含书名、笔记内容列）
- **输出**：JSON文件（可直接导入测验系统）

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| OPENAI_API_KEY | LLM API密钥 | - |
| OPENAI_API_BASE | API地址 | https://api.openai.com/v1 |
| LLM_MODEL | 模型名称 | gpt-3.5-turbo |
