import { McpServer } from '@modelcontextprotocol/server';
import { serveStdio } from '@modelcontextprotocol/server/stdio';
import { z } from 'zod';
import { MindpopApi, MindpopApiError } from './api.js';

const answerSchema = z.object({
  content: z.string().min(1),
  comment: z.string().optional(),
  formatVersion: z.number().int().optional(),
  parts: z.array(z.object({ segments: z.array(z.object({ kind: z.enum(['context', 'required']), text: z.string().min(1) }).strict()).min(1) }).strict()).min(1).optional(),
}).strict();
const quizInput = z.object({
  title: z.string().min(1), description: z.string().optional(), timeLimit: z.number().int().nonnegative().nullable().optional(),
  quizType: z.literal('TYPING').default('TYPING'), answerList: z.array(answerSchema).min(1), groups: z.array(z.string()).optional(),
}).strict();
const groupInput = z.object({ name: z.string().min(1), description: z.string().optional(), displayOrder: z.number().int().optional(), quizIds: z.array(z.number().int()).optional() }).strict();
const idInput = z.object({ id: z.number().int().positive() }).strict();
const versionInput = idInput.extend({ version: z.number().int().nonnegative() });

function result(data: unknown, summary: string) {
  return { content: [{ type: 'text' as const, text: summary }], structuredContent: data as Record<string, unknown> };
}
function failure(error: unknown) {
  if (error instanceof MindpopApiError) {
    const data = { code: error.code, message: error.message, details: error.details };
    return { isError: true, content: [{ type: 'text' as const, text: `${error.code}: ${error.message}` }], structuredContent: data };
  }
  const message = error instanceof Error ? error.message : String(error);
  return { isError: true, content: [{ type: 'text' as const, text: message }], structuredContent: { code: 'INTERNAL_ERROR', message, details: {} } };
}
function register(server: McpServer, api: MindpopApi) {
  const read = { readOnlyHint: true, destructiveHint: false };
  const write = { readOnlyHint: false, destructiveHint: false };
  const destructive = { readOnlyHint: false, destructiveHint: true };
  server.registerTool('list_quizzes', { description: '列出当前用户题库（支持关键词、分组、题型和分页）', inputSchema: z.object({ query: z.string().optional(), group_id: z.number().int().optional(), type: z.enum(['TYPING', 'FILL_BLANK']).optional(), page: z.number().int().nonnegative().default(0), size: z.number().int().min(1).max(100).default(20) }).strict(), annotations: read }, async (input) => { try { const data = await api.request('GET', `/quizzes?${new URLSearchParams(Object.entries({ query: input.query, groupId: input.group_id?.toString(), type: input.type, page: String(input.page), size: String(input.size) }).filter(([, v]) => v !== undefined) as [string, string][]).toString()}`); return result(data, `已找到 ${(data as any).items?.length ?? 0} 个题库`); } catch (e) { return failure(e); } });
  server.registerTool('get_quiz', { description: '读取题库详情、答案、分组和版本', inputSchema: idInput, annotations: read }, async ({ id }) => { try { const data = await api.request('GET', `/quizzes/${id}`); return result(data, `已读取题库 ${id}`); } catch (e) { return failure(e); } });
  server.registerTool('create_quiz', { description: '创建 TYPING 题库', inputSchema: quizInput, annotations: write }, async (input) => { try { const data = await api.request('POST', '/quizzes', input); return result(data, `已创建题库 ${(data as any).id}`); } catch (e) { return failure(e); } });
  server.registerTool('update_quiz', { description: '按版本更新 TYPING 题库的显式字段', inputSchema: versionInput.extend({ title: z.string().min(1).optional(), description: z.string().nullable().optional(), timeLimit: z.number().int().nonnegative().nullable().optional(), answerList: z.array(answerSchema).min(1).optional() }), annotations: write }, async (input) => { try { const { id, ...body } = input; const data = await api.request('PATCH', `/quizzes/${id}`, body); return result(data, `已更新题库 ${id}`); } catch (e) { return failure(e); } });
  server.registerTool('import_quizzes', { description: '原子、幂等导入 TYPING 题库；同一逻辑重试必须复用 request_id', inputSchema: z.object({ request_id: z.string().uuid(), quizzes: z.array(quizInput).min(1) }).strict(), annotations: write }, async ({ request_id: requestId, quizzes }) => { try { const data = await api.request('POST', '/imports', { requestId, quizzes }); return result(data, `导入完成，共 ${(data as any).count ?? 0} 个题库`); } catch (e) { return failure(e); } });
  server.registerTool('preview_delete_quiz', { description: '预览题库删除影响并取得五分钟确认令牌', inputSchema: idInput, annotations: read }, async ({ id }) => { try { const data = await api.request('POST', `/quizzes/${id}/delete-preview`); return result(data, `已生成题库 ${id} 删除预览，请确认后再删除`); } catch (e) { return failure(e); } });
  server.registerTool('delete_quiz', { description: '使用删除预览确认令牌删除题库', inputSchema: idInput.extend({ confirmation_token: z.string().min(1) }).strict(), annotations: destructive }, async ({ id, confirmation_token: confirmationToken }) => { try { await api.request('DELETE', `/quizzes/${id}`, { confirmationToken }); return result({ id, deleted: true }, `已删除题库 ${id}`); } catch (e) { return failure(e); } });
  server.registerTool('list_groups', { description: '列出当前用户分组', inputSchema: z.object({}).strict(), annotations: read }, async () => { try { const data = await api.request('GET', '/groups'); return result({ items: data }, `已找到 ${(data as any[]).length} 个分组`); } catch (e) { return failure(e); } });
  server.registerTool('get_group', { description: '读取分组及其题库 ID', inputSchema: idInput, annotations: read }, async ({ id }) => { try { const data = await api.request('GET', `/groups/${id}`); return result(data, `已读取分组 ${id}`); } catch (e) { return failure(e); } });
  server.registerTool('create_group', { description: '创建分组，可同时关联当前用户题库', inputSchema: groupInput, annotations: write }, async (input) => { try { const data = await api.request('POST', '/groups', input); return result(data, `已创建分组 ${(data as any).id}`); } catch (e) { return failure(e); } });
  server.registerTool('update_group', { description: '按版本更新分组显式字段', inputSchema: versionInput.extend({ name: z.string().min(1).optional(), description: z.string().nullable().optional(), displayOrder: z.number().int().optional() }), annotations: write }, async (input) => { try { const { id, ...body } = input; const data = await api.request('PATCH', `/groups/${id}`, body); return result(data, `已更新分组 ${id}`); } catch (e) { return failure(e); } });
  server.registerTool('preview_delete_group', { description: '预览分组删除影响并取得五分钟确认令牌', inputSchema: idInput, annotations: read }, async ({ id }) => { try { const data = await api.request('POST', `/groups/${id}/delete-preview`); return result(data, `已生成分组 ${id} 删除预览，请确认后再删除`); } catch (e) { return failure(e); } });
  server.registerTool('delete_group', { description: '使用删除预览确认令牌删除分组（题库保留）', inputSchema: idInput.extend({ confirmation_token: z.string().min(1) }).strict(), annotations: destructive }, async ({ id, confirmation_token: confirmationToken }) => { try { await api.request('DELETE', `/groups/${id}`, { confirmationToken }); return result({ id, deleted: true }, `已删除分组 ${id}，题库仍保留`); } catch (e) { return failure(e); } });
  const membershipInput = z.object({ group_id: z.number().int().positive(), quiz_id: z.number().int().positive() }).strict();
  server.registerTool('add_quiz_to_group', { description: '将题库加入分组', inputSchema: membershipInput, annotations: write }, async ({ group_id: groupId, quiz_id: quizId }) => { try { const data = await api.request('POST', `/groups/${groupId}/quizzes/${quizId}`); return result(data, `已将题库 ${quizId} 加入分组 ${groupId}`); } catch (e) { return failure(e); } });
  server.registerTool('remove_quiz_from_group', { description: '从分组解除题库关联', inputSchema: membershipInput, annotations: write }, async ({ group_id: groupId, quiz_id: quizId }) => { try { const data = await api.request('DELETE', `/groups/${groupId}/quizzes/${quizId}`); return result(data, `已将题库 ${quizId} 从分组 ${groupId} 移除`); } catch (e) { return failure(e); } });
}

export function createServer(api = new MindpopApi()) {
  const server = new McpServer({ name: 'mindpop', version: '0.1.0' });
  register(server, api);
  return server;
}

if (process.argv[1]?.endsWith('/dist/index.js')) {
  try {
    const api = new MindpopApi();
    serveStdio(() => createServer(api), { onerror: (error) => console.error(`[mindpop-mcp] ${error.message}`) });
  }
  catch (error) { console.error(`[mindpop-mcp] ${error instanceof Error ? error.message : String(error)}`); process.exitCode = 1; }
}
