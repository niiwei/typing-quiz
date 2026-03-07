const puppeteer = require('puppeteer');
const path = require('path');

(async () => {
    // 启动浏览器
    const browser = await puppeteer.launch();
    const page = await browser.newPage();

    // 设置视口为 375x500（宣传页尺寸）
    await page.setViewport({
        width: 375,
        height: 500,
        deviceScaleFactor: 2 // 2倍清晰度
    });

    // 6张宣传页的配置
    const cards = [
        { id: 1, name: '01-品牌介绍' },
        { id: 2, name: '02-强制提取' },
        { id: 3, name: '03-科学复习' },
        { id: 4, name: '04-分组管理' },
        { id: 5, name: '05-学习统计' },
        { id: 6, name: '06-自主创建' }
    ];

    // 依次截图每张宣传页
    for (let i = 0; i < cards.length; i++) {
        const card = cards[i];

        // 加载页面并滚动到对应卡片位置
        await page.goto(`file://${path.join(__dirname, 'promo-all.html')}`, {
            waitUntil: 'networkidle0'
        });

        // 滚动到对应卡片
        await page.evaluate((index) => {
            const cards = document.querySelectorAll('.promo-card');
            if (cards[index]) {
                cards[index].scrollIntoView({ block: 'start' });
            }
        }, i);

        // 等待渲染完成
        await page.waitForTimeout(500);

        // 截取当前可视区域
        await page.screenshot({
            path: `./promo-images/${card.name}.png`,
            clip: {
                x: 0,
                y: i * 540, // 每张卡片高度 + 间距
                width: 375,
                height: 500
            }
        });

        console.log(`✓ 已生成: ${card.name}.png`);
    }

    await browser.close();
    console.log('\n全部6张宣传页图片已生成到 promo-images 文件夹');
})();
