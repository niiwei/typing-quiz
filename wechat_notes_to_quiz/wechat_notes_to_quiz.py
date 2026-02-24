"""
微信读书笔记 → 填空题生成器
将微信读书导出的原始笔记(txt)直接转换为可导入测验系统的JSON文件
"""

import json
import os
import re
import subprocess
from pathlib import Path

# 配置区域（火山引擎豆包模型REST API）
API_KEY = "dd12cd7d-02e3-4812-addb-36f274402700"
API_BASE = os.getenv("ARK_API_BASE", "https://ark.cn-beijing.volces.com/api/v3")
MODEL = os.getenv("ARK_MODEL", "doubao-seed-1-8-251228")


def parse_wechat_reading_notes(input_text):
    """
    解析微信读书划线笔记
    返回: (书名, 笔记列表)
    """
    lines = input_text.strip().split('\n')
    
    # 第一行是书名
    book_name = lines[0].strip() if lines else ""
    # 移除书名号
    book_name = book_name.replace('《', '').replace('》', '')
    
    # 提取笔记：◆开头的行及其后续内容
    notes = []
    current_note_lines = []
    collecting = False
    empty_line_count = 0
    
    for line in lines[1:]:
        stripped = line.strip()
        
        # 检测空行
        if not stripped:
            empty_line_count += 1
            continue
        
        # 检测标题：连续两个及以上空行后的非空行视为标题
        if empty_line_count >= 2:
            if current_note_lines:
                notes.append('\n'.join(current_note_lines))
                current_note_lines = []
            collecting = False
        
        # 重置空行计数
        empty_line_count = 0
        
        # 检测 ◆ 开头的行（划线内容）
        if stripped.startswith('◆ '):
            # 保存上一条笔记
            if current_note_lines:
                notes.append('\n'.join(current_note_lines))
            # 开始新笔记（去掉◆）
            current_note_lines = [stripped[2:].strip()]
            collecting = True
        elif collecting:
            # 收集笔记的后续内容
            current_note_lines.append(stripped)
    
    # 保存最后一条笔记
    if current_note_lines:
        notes.append('\n'.join(current_note_lines))
    
    return book_name, notes


def call_llm(prompt):
    """调用LLM提取标题和关键词"""
    url = f"{API_BASE}/chat/completions"
    
    curl_cmd = [
        'curl', url,
        '-H', f'Authorization: Bearer {API_KEY}',
        '-H', 'Content-Type: application/json',
        '-d', json.dumps({
            "model": MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "keywords_extraction",
                    "strict": True,
                    "schema": {
                        "type": "object",
                        "properties": {
                            "title": {"type": "string"},
                            "keywords": {
                                "type": "array",
                                "items": {"type": "string"},
                                "minItems": 2,
                                "maxItems": 2
                            }
                        },
                        "required": ["title", "keywords"],
                        "additionalProperties": False
                    }
                }
            },
            "thinking": {"type": "disabled"}
        }, ensure_ascii=False)
    ]
    
    result = subprocess.run(curl_cmd, capture_output=True, text=True, encoding='utf-8')
    
    if result.returncode != 0:
        raise Exception(f"curl错误: {result.stderr}")
    
    if not result.stdout:
        raise Exception(f"响应为空")
    
    response_data = json.loads(result.stdout)
    choices = response_data.get("choices", [])
    if choices:
        return choices[0]["message"]["content"]
    
    raise Exception(f"无法解析响应")


def generate_fill_blank(note_content, book_name):
    """生成填空题"""
    prompt = f"""你是一个教育内容生成助手。

任务：
分析以下笔记，提取标题和2个核心关键词。

笔记内容：
{note_content}

要求：
1. 标题：简洁准确，不超过20字
2. 关键词：从笔记中提取2个核心概念词
3. 必须返回JSON格式：
{{"title": "标题", "keywords": ["关键词1", "关键词2"]}}

严格遵循上述格式。"""
    
    try:
        result = call_llm(prompt)
        
        # 提取JSON
        start = result.find('{')
        if start != -1:
            end = result.rfind('}') + 1
            json_str = result[start:end]
        else:
            return None
        
        data = json.loads(json_str)
        if isinstance(data, list):
            data = data[0] if len(data) > 0 else {}
        
        title = data.get("title", "")
        keywords = data.get("keywords", [])
        
        if len(keywords) < 2:
            return None
        
        # 在原文中定位关键词并生成挖空
        full_text = note_content
        blanks = []
        temp_text = full_text
        offset = 0
        
        for keyword in keywords:
            idx = temp_text.find(keyword)
            if idx != -1:
                blanks.append({
                    "startIndex": idx + offset,
                    "endIndex": idx + offset + len(keyword),
                    "correctAnswer": keyword,
                    "comment": None
                })
                temp_text = temp_text[:idx] + '____' + temp_text[idx + len(keyword):]
                offset += 4 - len(keyword)
        
        # 生成displayText
        display_text = full_text
        for keyword in keywords:
            display_text = display_text.replace(keyword, '____', 1)
        
        if len(blanks) == 0:
            return None
        
        return {
            "title": title,
            "description": None,
            "timeLimit": None,
            "answers": None,
            "answerList": None,
            "groups": [book_name],
            "quizType": "FILL_BLANK",
            "fillBlankQuiz": {
                "fullText": full_text,
                "displayText": display_text,
                "blanks": blanks,
                "blanksCount": len(blanks)
            }
        }
    except Exception as e:
        print(f"  ✗ 处理失败: {e}")
        return None


def process_txt_file(txt_path, output_dir):
    """处理单个txt文件"""
    print(f"\n处理文件: {txt_path.name}")
    
    # 读取txt
    with open(txt_path, 'r', encoding='utf-8') as f:
        input_text = f.read()
    
    # 解析笔记
    book_name, notes = parse_wechat_reading_notes(input_text)
    print(f"  书名: {book_name}")
    print(f"  笔记数量: {len(notes)}")
    
    if len(notes) == 0:
        print("  ✗ 没有解析到笔记")
        return
    
    # 生成填空题
    print(f"\n  生成填空题...")
    results = []
    for i, note in enumerate(notes, 1):
        note_preview = note[:30].replace('\n', ' ')
        print(f"    [{i:2}/{len(notes)}] {note_preview}...", end="")
        
        quiz = generate_fill_blank(note, book_name)
        if quiz:
            results.append(quiz)
            print(f" ✓ ({quiz['fillBlankQuiz']['blanksCount']}个空)")
        else:
            print(f" ✗")
    
    print(f"\n  ✓ 成功生成 {len(results)} 道填空题")
    
    # 保存JSON
    base_name = f"{book_name}_填空题.json"
    output_path = output_dir / base_name
    
    # 处理同名文件
    counter = 1
    while output_path.exists():
        output_path = output_dir / f"{book_name}_填空题({counter}).json"
        counter += 1
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print(f"  ✓ 保存到: {output_path}")


def main():
    script_dir = Path(__file__).parent
    input_dir = script_dir / "input"
    output_dir = script_dir / "output"
    
    # 创建目录
    input_dir.mkdir(exist_ok=True)
    output_dir.mkdir(exist_ok=True)
    
    # 检查input目录
    txt_files = list(input_dir.glob("*.txt"))
    
    if not txt_files:
        print("=" * 50)
        print("微信读书笔记 → 填空题生成器")
        print("=" * 50)
        print(f"\n请将微信读书导出的笔记(txt文件)放入:")
        print(f"  {input_dir}")
        print(f"\n然后重新运行程序")
        print("=" * 50)
        return
    
    print("=" * 50)
    print("微信读书笔记 → 填空题生成器")
    print("=" * 50)
    print(f"\n发现 {len(txt_files)} 个txt文件\n")
    
    # 批量处理
    for txt_file in txt_files:
        process_txt_file(txt_file, output_dir)
    
    print("\n" + "=" * 50)
    print(f"全部完成! 输出目录: {output_dir}")
    print("=" * 50)


if __name__ == "__main__":
    main()
