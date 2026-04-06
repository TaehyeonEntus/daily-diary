const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  try {
    console.log('http://localhost:3001 접속 중...');
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000); // 테마 전환 대기
    await page.screenshot({ path: 'screenshot.png', fullPage: true });
    console.log('스크린샷 저장 완료: screenshot.png');
  } catch (error) {
    console.error('오류 발생:', error);
  } finally {
    await browser.close();
  }
})();
