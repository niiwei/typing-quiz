@echo off
chcp 65001 >nul
cd /d "%~dp0"
python wechat_notes_to_quiz.py
pause
