const { test, expect } = require('@playwright/test');

test('front-end normalization ignores all configured whitespace', async ({ page }) => {
  await page.goto('/index.html');
  const result = await page.evaluate(() => {
    const controller = new QuizController(null);
    controller.settings = {
      ignorePunctuation: false,
      ignoreSpaces: true,
      ignoreCase: true,
    };
    return controller.normalizeText('Token\u3000\t数');
  });
  expect(result).toBe('token数');
});

test('front-end normalization preserves edge whitespace when disabled', async ({ page }) => {
  await page.goto('/index.html');
  const result = await page.evaluate(() => {
    const controller = new QuizController(null);
    controller.settings = {
      ignorePunctuation: false,
      ignoreSpaces: false,
      ignoreCase: true,
    };
    return controller.normalizeText(' Token数 ');
  });
  expect(result).toBe(' token数 ');
});
