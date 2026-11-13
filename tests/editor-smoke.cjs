const {chromium} = require('playwright-core');
const fs = require('fs');
const path = require('path');
const os = require('os');
const {pathToFileURL} = require('url');

(async () => {
  const browser = await chromium.launch({headless: true, executablePath: process.env.CHROME_BIN || '/usr/bin/google-chrome', args: ['--no-sandbox']});
  try {
    const page = await browser.newPage({viewport: {width: 390, height: 844}, isMobile: true, hasTouch: true});
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(pathToFileURL(path.resolve('app/src/main/assets/index.html')).href);
    await page.waitForSelector('#atelierImport');
    const png = await page.evaluate(() => {const c = document.createElement('canvas');c.width=80;c.height=50;const g=c.getContext('2d');g.fillStyle='#f24835';g.fillRect(0,0,80,50);return c.toDataURL('image/png').split(',')[1]});
    const dir=fs.mkdtempSync(path.join(os.tmpdir(),'glitch-'));
    const one=path.join(dir,'one.png'),two=path.join(dir,'two.png'),bad=path.join(dir,'bad.png');
    fs.writeFileSync(one,Buffer.from(png,'base64'));fs.copyFileSync(one,two);fs.writeFileSync(bad,'invalid image');
    await page.locator('#upload').setInputFiles([one,two]);
    await page.waitForFunction(()=>document.querySelector('#fileInfo').textContent.includes('one.png'));
    await page.waitForFunction(()=>document.querySelector('#statusLabel').textContent==='SIGNAL COMPOSED');
    await page.screenshot({path:'/tmp/glitch-ui-image.png'});
    if (!(await page.locator('#batchCount').innerText()).startsWith('2 ')) throw Error('Missing images in queue');
    await page.locator('#atelierQueue').click();
    await page.locator('.queue-item').nth(1).getByText('Podgląd').click();
    await page.waitForFunction(()=>document.querySelector('#fileInfo').textContent.includes('two.png'));
    await page.locator('#upload').setInputFiles(bad);
    await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Nie udało się odczytać'));
    if (!(await page.locator('#batchCount').innerText()).startsWith('2 ')) throw Error('Queue lost after invalid image');
    await page.locator('#upload').setInputFiles(one);
    await page.waitForFunction(()=>document.querySelector('#fileInfo').textContent.includes('one.png'));
    await page.locator('.atelier-nav [data-panel="effects"]').click();
    await page.screenshot({path:'/tmp/glitch-ui-effects.png'});
    await page.locator('#stackPalette .add-chip').first().click();
    await page.locator('.atelier-nav [data-panel="stack"]').click();
    if (await page.locator('#stackList .stack-item').count() !== 1) throw Error('Effect not added to stack');
    await page.screenshot({path:'/tmp/glitch-ui-stack.png'});
    await page.locator('#atelierClose').click();
    await page.locator('#atelierCamera').click();
    const camera=page.frameLocator('iframe[title="Oryginalny GLITCH CAMERA Pocket Lab"]');
    await camera.locator('#demo').click();
    await camera.locator('#sendEditor').waitFor({state:'visible'});
    await page.waitForFunction(()=>{const f=document.querySelector('iframe[title="Oryginalny GLITCH CAMERA Pocket Lab"]');return f&&f.contentDocument.querySelector('#sendEditor')&&!f.contentDocument.querySelector('#sendEditor').disabled});
    await camera.locator('#sendEditor').click();
    await page.waitForFunction(()=>document.querySelector('#batchCount').textContent.startsWith('2 '));
    if(errors.length) throw Error('Page errors: '+errors.join('; '));
    console.log('Image selection, queue, error recovery and camera transfer passed.');
  } finally {await browser.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
