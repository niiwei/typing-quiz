import test from 'node:test';
import assert from 'node:assert/strict';
import { MindpopApi, MindpopApiError } from '../dist/api.js';

test('sends PAT bearer header and parses JSON', async () => {
  const previous = globalThis.fetch;
  let seen;
  globalThis.fetch = async (url, options) => {
    seen = { url, options };
    return new Response(JSON.stringify({ items: [] }), { status: 200, headers: { 'content-type': 'application/json' } });
  };
  try {
    const api = new MindpopApi('https://mindpop.test/', 'mp_pat_secret');
    assert.deepEqual(await api.request('GET', '/quizzes'), { items: [] });
    assert.equal(seen.url, 'https://mindpop.test/api/agent/v1/quizzes');
    assert.equal(seen.options.headers.Authorization, 'Bearer mp_pat_secret');
  } finally { globalThis.fetch = previous; }
});

test('maps stable API errors without exposing token', async () => {
  const previous = globalThis.fetch;
  globalThis.fetch = async () => new Response(JSON.stringify({ code: 'VERSION_CONFLICT', message: '版本已变化', details: { version: 2 } }), { status: 409 });
  try {
    const api = new MindpopApi('https://mindpop.test', 'mp_pat_secret');
    await assert.rejects(() => api.request('PATCH', '/quizzes/1', {}), (error) => {
      assert.ok(error instanceof MindpopApiError);
      assert.equal(error.code, 'VERSION_CONFLICT');
      assert.doesNotMatch(error.message, /mp_pat_secret/);
      return true;
    });
  } finally { globalThis.fetch = previous; }
});

test('fails with actionable missing environment errors', () => {
  assert.throws(() => new MindpopApi(undefined, 'x'), /MINDPOP_BASE_URL/);
  assert.throws(() => new MindpopApi('https://mindpop.test', undefined), /MINDPOP_PAT/);
});
