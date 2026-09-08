const { test, expect } = require('@playwright/test');

test('typing editor parses context, required text, parts, and comment separately', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('typingquiz_token', 'browser-test-token'));
  await page.goto('/create.html');
  const parsed = await page.evaluate(() =>
    parseTypingAnswerWithComment('{{发布 }}llms.txt{{ 文档索引}}#AI 文档入口#', 1)
  );
  expect(parsed.formatVersion).toBe(2);
  expect(parsed.content).toBe('发布 llms.txt 文档索引');
  expect(parsed.comment).toBe('AI 文档入口');
  expect(parsed.parts[0].segments).toEqual([
    { kind: 'context', text: '发布 ' },
    { kind: 'required', text: 'llms.txt' },
    { kind: 'context', text: ' 文档索引' },
  ]);

  const escaped = await page.evaluate(() =>
    parseTypingAnswerWithComment('{{a\\}\\}b}}key', 2)
  );
  expect(escaped.content).toBe('a}}bkey');
  expect(escaped.parts[0].segments).toEqual([
    { kind: 'context', text: 'a}}b' },
    { kind: 'required', text: 'key' },
  ]);
});
