const { test, expect } = require('@playwright/test');

test('typing quiz page loads its real input surface', async ({ page }) => {
  const baseURL = process.env.MINDPOP_BROWSER_BASE_URL || 'http://127.0.0.1:8080';
  await page.goto(`${baseURL}/index.html`);
  await expect(page.locator('#answer-input')).toBeVisible();
});
