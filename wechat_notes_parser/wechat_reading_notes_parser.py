"""
微信读书划线笔记解析工具
将微信读书导出的划线笔记解析为CSV格式，每条笔记占一行
"""

import re
import csv


def parse_wechat_reading_notes(input_text):
    """
    解析微信读书划线笔记
    
    Args:
        input_text: 输入的笔记文本
        
    Returns:
        tuple: (书名, 笔记列表)
    """
    lines = input_text.strip().split('\n')
    
    # 第一行是书名
    book_name = lines[0].strip() if lines else ""
    
    # 提取笔记：◆开头的行及其后续内容
    notes = []
    current_note_lines = []
    collecting = False
    empty_line_count = 0  # 连续空行计数
    
    for line in lines[1:]:
        stripped = line.strip()
        
        # 检测空行
        if not stripped:
            empty_line_count += 1
            continue
        
        # 检测标题：连续两个及以上空行后的非空行视为标题
        if empty_line_count >= 2:
            # 保存当前笔记
            if current_note_lines:
                notes.append('\n'.join(current_note_lines))
                current_note_lines = []
            collecting = False
        
        # 重置空行计数
        empty_line_count = 0
        
        # 检测 ◆ 开头的行（划线内容或日期）
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


def save_to_csv(book_name, notes, output_path=None):
    """
    保存笔记到CSV文件
    """
    if output_path is None:
        safe_name = re.sub(r'[\\/:*?"<>|]', '_', book_name)
        output_path = f"{safe_name}_笔记.csv"
    
    with open(output_path, 'w', encoding='utf-8-sig', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['书名', '笔记内容'])
        for note in notes:
            writer.writerow([book_name, note])
    
    return output_path


def main():
    import sys
    from pathlib import Path
    
    # 获取脚本所在目录
    script_dir = Path(__file__).parent
    input_file = script_dir / "input.txt"
    
    print("=" * 50)
    print("微信读书划线笔记解析工具")
    print("=" * 50)
    
    # 检查是否有传入文件参数
    if len(sys.argv) > 1:
        # 从文件读取
        input_path = Path(sys.argv[1])
        print(f"[1/4] 读取文件: {input_path}")
        with open(input_path, 'r', encoding='utf-8') as f:
            input_text = f.read()
        print(f"       文件大小: {len(input_text)} 字符")
    elif input_file.exists():
        # 从子文件夹的 input.txt 读取
        print(f"[1/4] 读取文件: {input_file}")
        with open(input_file, 'r', encoding='utf-8') as f:
            input_text = f.read()
        print(f"       文件大小: {len(input_text)} 字符")
    elif not sys.stdin.isatty():
        # 从管道/标准输入读取
        print("[1/4] 从标准输入读取...")
        input_text = sys.stdin.read()
        print(f"       输入大小: {len(input_text)} 字符")
    else:
        # 提示用户
        print("[1/4] 使用方式:")
        print(f"       将笔记保存到: {input_file}")
        print("       然后运行: python wechat_reading_notes_parser.py")
        print("-" * 50)
        return
    
    print("[2/4] 解析笔记...")
    book_name, notes = parse_wechat_reading_notes(input_text)
    print(f"       书名: {book_name}")
    print(f"       笔记数量: {len(notes)}")
    
    print("[3/4] 保存CSV...")
    output_path = save_to_csv(book_name, notes)
    print(f"       保存到: {output_path}")
    
    print("[4/4] 完成!")
    print("=" * 50)


if __name__ == "__main__":
    main()
