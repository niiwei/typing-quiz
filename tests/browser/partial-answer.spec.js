const { test, expect } = require('@playwright/test');

test('partial answer display keeps blanks hidden and shared keyword completes all matches', async ({ page }) => {
  await page.goto('/index.html');
  const result = await page.evaluate(async () => {
    const controller = new QuizController(null);
    controller.answers = [
      {
        id: 1,
        content: '降低成本、提高效率',
        formatVersion: 2,
        parts: [
          { segments: [{ kind: 'required', text: '降低成本' }, { kind: 'context', text: '、' }] },
          { segments: [{ kind: 'required', text: '提高效率' }] },
        ],
      },
      {
        id: 2,
        content: '提高效率',
        formatVersion: 2,
        parts: [{ segments: [{ kind: 'required', text: '提高效率' }] }],
      },
    ];
    controller.foundAnswers = new Set();
    controller.foundParts = new Map();
    controller.settings.ignorePunctuation = false;
    UIRenderer.renderAnswersGrid(controller.answers, controller.foundAnswers, true, controller.foundParts);
    const initial = document.querySelector('#answers-grid').textContent;
    controller.applyAnswerMatches([{ answerId: 1, partIndices: [0] }]);
    const partial = document.querySelector('#answers-grid').textContent;
    const partialSegmentClasses = Array.from(document.querySelectorAll('#answer-1 .answer-content > span'))
      .map(element => element.className);
    await controller.checkAnswer('降低成本提高效率');
    const requiredOnlyComplete = document.querySelector('#answer-1').textContent;
    const completeSegmentClasses = Array.from(document.querySelectorAll('#answer-1 .answer-content > span'))
      .map(element => element.className);
    UIRenderer.showAllAnswers(controller.answers, new Set(), new Map(), true);
    const missedRequiredColor = getComputedStyle(document.querySelector('#answer-1 .answer-required')).color;
    controller.saveToLocalHistory = () => {};
    controller.saveRecordToServer = () => {};
    await controller.checkAnswer('提高效率');
    await new Promise(resolve => setTimeout(resolve, 20));
    return {
      initial,
      partial,
      partialSegmentClasses,
      requiredOnlyComplete,
      completeSegmentClasses,
      missedRequiredColor,
      complete: document.querySelector('#answer-1').textContent,
      shared: document.querySelector('#answer-2').textContent,
      score: document.querySelector('#score-display').textContent,
      found: controller.foundAnswers.size,
    };
  });
  expect(result.initial).toBe('••');
  expect(result.partial).toContain('降低成本、______');
  expect(result.partial).not.toContain('提高效率');
  expect(result.partialSegmentClasses).toEqual(['answer-required', 'answer-context']);
  expect(result.requiredOnlyComplete).toContain('降低成本、提高效率');
  expect(result.completeSegmentClasses).toEqual(['answer-required', 'answer-context', 'answer-required']);
  expect(result.missedRequiredColor).toBe('rgb(220, 38, 38)');
  expect(result.complete).toContain('降低成本、提高效率');
  expect(result.shared).toContain('提高效率');
  expect(result.score).toBe('2/2');
  expect(result.found).toBe(2);
});
