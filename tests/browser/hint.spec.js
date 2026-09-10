const { test, expect } = require('@playwright/test');

test('typing hint reveals the next answer without throwing', async ({ page }) => {
  const pageErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  await page.goto('/index.html');

  await page.evaluate(() => {
    const controller = new QuizController(null);
    controller.isQuizActive = true;
    controller.quizType = 'TYPING';
    controller.answers = [{ id: 1, content: 'llms.txt', comment: '模型可读文档索引' }];
    controller.foundAnswers = new Set();
    controller.foundParts = new Map();
    UIRenderer.renderAnswersGrid(controller.answers, controller.foundAnswers, true, controller.foundParts);
    window.quizController = controller;
  });
  await page.locator('#hint-btn').click();

  const result = await page.evaluate(() => {
    const controller = window.quizController;

    return {
      found: Array.from(controller.foundAnswers),
      score: document.querySelector('#score-display').textContent,
      rendered: document.querySelector('#answer-1').textContent,
    };
  });

  expect(pageErrors).toEqual([]);
  expect(result.found).toEqual([1]);
  expect(result.score).toBe('1/1');
  expect(result.rendered).toContain('llms.txt');
});

test('typing hint completes the remaining parts of a v2 answer', async ({ page }) => {
  await page.goto('/index.html');

  await page.evaluate(() => {
    const controller = new QuizController(null);
    controller.isQuizActive = true;
    controller.quizType = 'TYPING';
    controller.answers = [{
      id: 7,
      content: '降低成本、提高效率',
      formatVersion: 2,
      parts: [
        { segments: [{ kind: 'required', text: '降低成本' }, { kind: 'context', text: '、' }] },
        { segments: [{ kind: 'required', text: '提高效率' }] },
      ],
    }];
    controller.foundAnswers = new Set();
    controller.foundParts = new Map([[7, new Set([0])]]);
    UIRenderer.renderAnswersGrid(controller.answers, controller.foundAnswers, true, controller.foundParts);
    window.quizController = controller;
  });
  await page.locator('#hint-btn').click();

  const result = await page.evaluate(() => {
    const controller = window.quizController;

    return {
      found: Array.from(controller.foundAnswers),
      parts: Array.from(controller.foundParts.get(7) || []),
      rendered: document.querySelector('#answer-7').textContent,
    };
  });

  expect(result.found).toEqual([7]);
  expect(result.parts).toEqual([0, 1]);
  expect(result.rendered).toContain('降低成本、提高效率');
});
