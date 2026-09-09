# 敲脑壳本地 MCP v1

Status: completed

## Problem Statement

题库整理 Skill 已能生成并校验 MindPop TYPING JSON，但用户仍需手动打开敲脑壳并导入文件。现有网站虽有题库与分组 API，却没有适合 Agent 长期使用的凭证、稳定的版本化接口和安全的重试语义；部分个人数据接口在缺少身份时还会跳过用户归属检查。因此，Agent 不能可靠、安全地完成题库和分组的增删改查。

## Solution

提供一个由 Codex 启动的本地 stdio MCP Server。它使用用户在敲脑壳设置页创建的个人访问令牌调用版本化 Agent API，让 Agent 能管理 TYPING 题库和分组，并把已确认、已校验的 Skill 输出原子导入网站。

网站先建立统一身份边界：网页使用现有 JWT，Agent API 使用 PAT，所有个人资源都在服务层按用户隔离。写入支持幂等导入、乐观并发控制和两步永久删除。Skill 只有在用户明确要求导入时才调用 MCP，成功后回读核验。

## User Stories

1. As a MindPop user, I want to create several named PATs, so that each local Agent or computer can be revoked independently.
2. As a MindPop user, I want a new PAT shown only once, so that the server never needs to reveal stored credentials.
3. As a MindPop user, I want to see creation and last-used times, so that I can recognize stale credentials.
4. As a MindPop user, I want to revoke one PAT without affecting my web login or other PATs.
5. As a web user, I want existing JWT login behavior to continue after the security change.
6. As a user, I want unauthenticated requests rejected before personal data is read or changed.
7. As a user, I want another account to receive no information about whether my resource exists.
8. As a PAT holder, I want access limited to the Agent API, so that the token cannot manage credentials or use admin endpoints.
9. As an Agent, I want to list quizzes with filters and bounded pagination, so that I can select an exact target.
10. As an Agent, I want all quiz types visible with a writable flag, so that I do not mistake an unsupported type for an editable one.
11. As an Agent, I want to read one quiz with its answers and groups, so that I can verify current state before acting.
12. As an Agent, I want to create a TYPING quiz using the Skill JSON shape.
13. As an Agent, I want to update only explicitly supplied quiz fields, so that omitted content remains unchanged.
14. As a user, I want stale updates rejected, so that an Agent cannot silently overwrite a newer edit.
15. As an Agent, I want to import several quizzes atomically, so that an invalid item cannot leave a half-imported batch.
16. As an Agent, I want to retry the same import request safely, so that timeouts do not create duplicates.
17. As a user, I want a later deliberate import of identical content treated as a new request.
18. As an Agent, I want missing named groups created during import, so that Skill output can be imported in one action.
19. As a user, I want group names unique within my account, so that name-based JSON association is unambiguous.
20. As an Agent, I want to preview a quiz deletion before confirming it, so that the exact target and impact are visible.
21. As a user, I want an expired or stale deletion confirmation rejected.
22. As an Agent, I want complete group CRUD and quiz membership operations.
23. As a user, I want deleting a group to keep its quizzes.
24. As a Codex user, I want the MCP Server to start through standard stdio configuration.
25. As a Codex user, I want read and write tools clearly annotated, so that Codex can request approval for writes.
26. As an Agent, I want structured tool results and stable errors, so that I can verify outcomes without parsing prose.
27. As a Skill user, I want “生成题库” to keep producing a local JSON file without changing website data.
28. As a Skill user, I want “生成并导入敲脑壳” to validate, import and verify the result without manual upload.
29. As a Skill user, I want a failed import to retain the validated JSON and report the actionable cause.

## Implementation Decisions

- The system remains one domain context. “PAT”, “Agent API”, “import request”, “deletion preview”, and “writable quiz” use the definitions in the domain glossary.
- The MCP boundary is a local TypeScript stdio server over the versioned HTTPS Agent API. It never accesses the database directly.
- The MCP runtime uses the stable official TypeScript SDK, ESM output and Node.js 20 or later. Standard output is reserved for MCP protocol frames; diagnostics use standard error.
- Codex is the v1 host. The server remains standard stdio MCP, but other host-specific setup is deferred.
- The web API accepts JWT. The Agent API accepts PAT. PATs are long-lived, named, revocable and fixed to full Agent data access; they cannot manage PATs or access web/admin APIs.
- PAT secrets are random high-entropy values. The complete value is returned once and only a hash is persisted. Local Codex configuration stores the selected PAT in plaintext by explicit user choice and must remain outside Git.
- Public web pages, registration, login and existing anonymous tracking remain public. Other personal-data APIs require valid identity.
- Resource ownership is enforced in the service layer. Missing or invalid identity returns 401; authenticated cross-user access returns 404.
- Group names are unique per user after trimming and case-insensitive comparison. Existing duplicates are reported by a read-only preflight and are never merged automatically.
- Quiz list defaults to creation-time descending, page 0, size 20, with maximum size 100; it supports keyword, group and type filters.
- All quiz types may be listed and read. Only TYPING supports Agent create, update, import and delete in v1; unsupported writes return a stable error.
- Quiz and group updates are partial and require the current version. Conflicts return 409 with the latest summary.
- Atomic imports require a caller-generated UUID requestId. The uniqueness boundary is user plus requestId. A replay with the same payload returns the original result; a different payload with the same requestId returns 409.
- Import validates the whole batch before writing, creates missing groups in the same transaction, creates review state, and rolls back every change on failure. The existing web import contract remains compatible.
- Quiz and group deletion require a preview-issued, user-bound, resource-bound, version-bound confirmation token that expires after five minutes. Group deletion removes memberships but preserves quizzes.
- Agent API errors use stable code, message and details fields. MCP maps them to structured tool errors without exposing credentials.
- The MCP exposes list/get/create/update/import, deletion preview/delete, complete group CRUD, and group membership tools. It marks reads and destructive operations with MCP tool annotations.
- The Skill keeps its current preview, user confirmation, file generation and deterministic validation. It calls import_quizzes only when import intent is explicit, reuses requestId for the same retry, and verifies returned quiz IDs through get_quiz.

## Testing Decisions

- Java behavior is tested at the HTTP boundary with Spring Boot, MockMvc and the isolated H2 profile, following the existing baseline integration test.
- Security tests observe status and response data through APIs; they do not assert filter or repository internals.
- PAT tests observe one-time disclosure, revocation and access behavior; a narrowly scoped persistence assertion may verify that plaintext is absent because this is the security invariant itself.
- Agent API tests cover authorization, ownership, filters, TYPING write boundaries, atomic rollback, idempotency, version conflicts, group uniqueness and two-step deletion.
- MCP tests use its public stdio/tool interface against a fake HTTP server. They verify registered tools, schemas, headers, structured results, error mapping and clean stdout.
- Browser tests cover the PAT settings workflow and retain the existing page regression suite.
- Final acceptance uses Codex against a local application to perform generate/import, readback, update, grouping and two-step deletion.

## Out of Scope

- Remote or Streamable HTTP MCP, OAuth and hosted ChatGPT web access.
- FILL_BLANK creation, update, import or deletion through Agent tools.
- Learning sessions, answers, review plans, progress and statistics as MCP tools.
- Permanent content-based deduplication or title-based overwrite.
- macOS Keychain integration.
- Push, deployment or operations against production data.

## Further Notes

- Baseline fixed point: `4b5d048dbb19538f7c4d3477f69901efb000d811` on `feature/answer-keypoints-v2`; the worktree was clean before planning documents were added.
- Java 11 container baseline on 2026-09-09: 6 tests passed, 0 failures, 0 errors, 0 skipped.
- Browser baseline was run against the isolated static app on 2026-09-09: 9 tests passed, 0 failed.
- The local machine has Node.js 22.23.0. Java verification currently uses Docker because no local Java Runtime is available.

## Delivery evidence

- Java 11 Docker `mvn test`: passed (including AgentApiIntegrationTest).
- Playwright browser suite: 9/9 passed.
- MCP `npm test`: 3/3 passed; direct stdio initialize/tools/list handshake returned all 15 tools.
- MCP Inspector CLI completed `tools/list`, `list_quizzes` error mapping, and invalid `create_quiz` schema validation.
- Skill Creator `quick_validate.py`: passed.
- Codex 本地端到端已完成：使用一次性 Docker MySQL 和临时注册用户，通过 stdio MCP 完成导入 1 题、回读 1 个答案、按版本修改、分组关联、预览并删除分组、确认题库仍可读，再预览并删除题库；随后清理临时容器，未接触线上数据。
