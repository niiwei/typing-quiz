const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests/browser',
  timeout: 15000,
  use: {
    baseURL: process.env.MINDPOP_BROWSER_BASE_URL || 'http://127.0.0.1:8080',
    browserName: 'chromium',
    headless: true,
  },
  reporter: [['list']],
});
