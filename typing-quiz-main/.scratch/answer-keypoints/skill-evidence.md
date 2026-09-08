# Skill v2 修改证据

全局文件仍位于 `/Users/yangxiaowen/.codex/skills/mindpop-quiz-builder/`，没有把全局目录复制进本仓库，也没有复制私密文章。仓库保存了修改补丁、样例和本次验证记录。

## SHA-256

| 文件 | 修改前 | 修改后 |
|---|---|---|
| `SKILL.md` | `e2c0d3b06f5373e34aaf8c8f5f7e0c5111e8a562dc9e7e7c4b4c4e1cc16923b7` | `e1142436f7e17982ee4183ab6a7096761a36d6647285f9092b47f2f5d767c3ef` |
| `scripts/validate_quiz_json.py` | `03fe23674c748529d312c5bbafc9f2adb531d37803f91e59bb78cf3503ee9048` | `06e02a14a0f7f8f62f590fb3a6003271575a574dd1584aa7ef457b535b7ff934` |

## 可复现命令

```bash
python3 /Users/yangxiaowen/.codex/skills/mindpop-quiz-builder/scripts/validate_quiz_json.py \
  typing-quiz-main/.scratch/answer-keypoints/skill-valid-sample.json
# VALID: MindPop TYPING quiz JSON

python3 /Users/yangxiaowen/.codex/skills/mindpop-quiz-builder/scripts/validate_quiz_json.py \
  typing-quiz-main/.scratch/answer-keypoints/skill-invalid-sample.json
# INVALID: part must contain at least one required segment

/tmp/mindpop-skill-venv/bin/python \
  /Users/yangxiaowen/.codex/skills/.system/skill-creator/scripts/quick_validate.py \
  /Users/yangxiaowen/.codex/skills/mindpop-quiz-builder
# exit 0
```

样例包含六个原始示例、`降低成本、提高效率` 的双 part，以及重复的 `提高效率` v2 条目；没有同义词扩展。产品验收由 `QuizBaselineIntegrationTest.importsAnswersFromSkillSampleAnswersExportsAndReimports` 完成：样例导入 1 个测验、8 个答案，正序、逆序、整句候选均命中，导出和再次读取保留双 part；无效样例被拒绝。

后续边界回归：Java 集成测试仍为 6/6；Playwright 页面回归为 7/7，覆盖旧版字面量保护、转义闭合标记、标点关闭时的必答词拼接、边缘空白保留、共享要点同时完成和安全文本渲染。
