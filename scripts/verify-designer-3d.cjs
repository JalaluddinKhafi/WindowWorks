// Optional browser smoke test: PLAYWRIGHT_MODULE can point to playwright/patchright-core.
// Runs entirely from bundled app assets; no web service or CDN is contacted.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const root = path.resolve(__dirname, '../app/src/main/assets/designer');
  const output = path.resolve(__dirname, '../app/build/reports/designer');
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ headless: true,
    executablePath: process.env.CHROMIUM_EXECUTABLE || undefined,
    args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  try {
    const page = await browser.newPage({ viewport: { width: 900, height: 650 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    await page.route('**/*', async route => {
      const url = new URL(route.request().url());
      assert.equal(url.origin, 'https://designer.windowworks.invalid');
      const file = path.resolve(root, '.' + url.pathname);
      assert.ok(file.startsWith(root + path.sep));
      await route.fulfill({ path: file, contentType: file.endsWith('.js') ? 'application/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html' });
    });
    await page.goto('https://designer.windowworks.invalid/index.html');
    await page.waitForFunction(() => typeof window.applyWindow === 'function');
    const faces = [];
    function box(x, y, z, w, h, d, color) {
      const a = [x,y,z], b = [x+w,y,z], c = [x+w,y+h,z], e = [x,y+h,z];
      const f = [x,y,z+d], g = [x+w,y,z+d], i = [x+w,y+h,z+d], j = [x,y+h,z+d];
      for (const vertices of [[a,b,c,e],[f,g,i,j],[a,f,j,e],[b,g,i,c],[a,b,g,f],[e,c,i,j]]) faces.push({ vertices, color, glass: false });
    }
    const frame = [0.83, 0.87, 0.92];
    box(-900,-1000,-35,1800,60,70,frame); box(-900,940,-35,1800,60,70,frame);
    box(-900,-940,-35,60,1880,70,frame); box(840,-940,-35,60,1880,70,frame);
    box(-37.5,-940,-35,75,1880,70,frame); box(37.5,300,-35,802.5,75,70,frame);
    for (const [x,y,w,h] of [[-840,-940,802.5,1880],[37.5,-940,802.5,1240],[37.5,375,802.5,565]]) {
      faces.push({ vertices: [[x,y,0],[x+w,y,0],[x+w,y+h,0],[x,y+h,0]], color: [0.22,0.74,0.97], glass: true });
    }
    const payload = { extent: 2000, yaw: -25, pitch: 15, zoom: 1, perspective: true, resetVersion: 0, faces };
    await page.evaluate(data => window.applyWindow(data), payload);
    const before = await page.screenshot({ path: path.join(output, 'three-preview.png') });
    await page.mouse.move(450, 300); await page.mouse.down(); await page.mouse.move(570, 350, { steps: 12 }); await page.mouse.up();
    await page.waitForTimeout(300);
    const after = await page.screenshot();
    assert.notDeepEqual(before, after, 'OrbitControls should change the view');
    await page.evaluate(data => window.applyWindow({ ...data, perspective: false, resetVersion: 1 }), payload);
    await page.screenshot({ path: path.join(output, 'three-orthographic.png') });
    assert.deepEqual(errors, []);
    console.log('PASS: local modules, WebGL, mesh rendering, orbit and orthographic projection.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
