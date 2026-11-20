const path = require('path');
const puppeteer = require('puppeteer-core');
const fs = require('fs');

const outDir = path.resolve(__dirname, '../docs/images');
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

async function run() {
  console.log('Launching Chrome from:', chromePath);
  const browser = await puppeteer.launch({
    executablePath: chromePath,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-web-security', '--ignore-certificate-errors', '--window-size=1440,900']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  const targets = [
    { name: 'storefront_home.png', url: 'https://quickshop.io.vn/' },
    { name: 'auth_login.png', url: 'https://quickshop.io.vn/login' },
    { name: 'intro_3d.png', url: 'https://quickshop.io.vn/intro' },
    { name: 'cart_page.png', url: 'https://quickshop.io.vn/cart' },
    { name: 'seller_portal.png', url: 'https://quickshop.io.vn/seller' },
    { name: 'admin_portal.png', url: 'https://quickshop.io.vn/admin' },
    { name: 'shipper_portal.png', url: 'https://quickshop.io.vn/shipper' },
    { name: 'messenger_chat.png', url: 'https://quickshop.io.vn/messenger' }
  ];

  for (const t of targets) {
    console.log(`Navigating to ${t.url} ...`);
    try {
      await page.goto(t.url, { waitUntil: 'networkidle2', timeout: 30000 });
      await new Promise(r => setTimeout(r, 2500));
      const targetPath = path.join(outDir, t.name);
      await page.screenshot({ path: targetPath, fullPage: false });
      console.log(`Saved screenshot: ${targetPath}`);
    } catch (e) {
      console.error(`Error on ${t.url}:`, e.message);
    }
  }

  await browser.close();
  console.log('Done capturing screenshots!');
}

run().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
