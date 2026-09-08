# 关键词作答任务入口

当前：01–04 已完成并有真实测试证据；05 已解锁。

- [规格](spec.md)
- [用户共识记录](design.md)
- [执行前证据](readiness.md)

| 票 | 交付 | 依赖 |
|---|---|---|
| [01](issues/01-test-baseline.md) | 隔离验证入口与旧题基线 | 已完成 |
| [02](issues/02-whitespace.md) | 真正忽略内部空白 | 已完成 |
| [03](issues/03-inline-context.md) | 免输入正文完整编辑作答闭环 | 已完成 |
| [04](issues/04-partial-answer.md) | 多要点逐段作答及结算复习闭环 | 已完成 |
| [05](issues/05-quiz-builder.md) | Skill 生成到实际产品验收 | 04 |

第一张票是环境与可验证基线，不是大规模重构；之后每票均交付完整用户路径。无并行写入或自动续票。

## 维护规则

票的 Status 使用默认分诊角色；另加 Execution: in-progress / complete 记录执行进度，依赖只有在 complete 且验收证据齐全时解除。ready-for-agent 不代表依赖已满足。保留完成票，不删除历史。
