const { test, expect } = require('@playwright/test');

test('legacy editor mode keeps literal v2 markers unchanged', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('typingquiz_token', 'browser-test-token'));
  await page.goto('/create.html');
  const result = await page.evaluate(() => {
    typingEditorMode = 'legacy';
    const answer = parseLegacyTypingAnswer('{{literal}}#说明#', 1);
    typingEditorMode = 'v2';
    const serializedForUpgrade = serializeTypingAnswerForEditor({
      content: '{{literal}}',
      comment: '说明',
    });
    typingEditorMode = 'legacy';
    const textarea = document.querySelector('#quiz-answers');
    textarea.value = '{{literal}}#说明#';
    updateTypingPreview();
    return {
      answer,
      serializedForUpgrade,
      preview: document.querySelector('#typing-preview').textContent,
    };
  });
  expect(result.answer).toEqual({ content: '{{literal}}', comment: '说明' });
  expect(result.serializedForUpgrade).toBe('\\{\\{literal\\}\\}#说明#');
  expect(result.preview).toBe('{{literal}}#说明#');
});
