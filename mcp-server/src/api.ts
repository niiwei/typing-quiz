export class MindpopApiError extends Error {
  constructor(readonly status: number, readonly code: string, message: string, readonly details: unknown = {}) {
    super(message);
  }
}

export class MindpopApi {
  private readonly baseUrl: string;
  constructor(baseUrl = process.env.MINDPOP_BASE_URL, private readonly pat = process.env.MINDPOP_PAT) {
    if (!baseUrl) throw new Error('缺少 MINDPOP_BASE_URL，请设置敲脑壳服务地址');
    if (!pat) throw new Error('缺少 MINDPOP_PAT，请先创建并配置 Agent 访问令牌');
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const response = await fetch(`${this.baseUrl}/api/agent/v1${path}`, {
      method,
      headers: { Authorization: `Bearer ${this.pat}`, 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await response.text();
    let data: any = {};
    try { data = text ? JSON.parse(text) : {}; } catch { data = {}; }
    if (!response.ok) {
      throw new MindpopApiError(response.status, data.code ?? 'HTTP_ERROR', data.message ?? `请求失败（${response.status}）`, data.details ?? {});
    }
    return data as T;
  }
}
