const { test, expect } = require('@playwright/test');

test('typing answers and comments render as text', async ({ page }) => {
  await page.goto('/index.html');
  const result = await page.evaluate(() => {
    const answer = {
      id: 1,
      content: '<img src=x onerror=alert(1)>',
      comment: '<b>comment</b>',
    };
    const found = new Set([1]);
    UIRenderer.renderAnswersGrid([answer], found, true, new Map());
    const item = document.querySelector('#answer-1');
    return {
      imageCount: item.querySelectorAll('img').length,
      text: item.textContent,
    };
  });
  expect(result.imageCount).toBe(0);
  expect(result.text).toBe('<img src=x onerror=alert(1)>#<b>comment</b>#');
});
