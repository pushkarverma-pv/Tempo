const {
  check, email, getFailures, login, newSession, openBrowser, password, register, url
} = require('./helpers.cjs');

async function run() {
  const browser = await openBrowser();
  const { context, page } = await newSession(browser);
  const address = email('auth');
  try {
    await check('GET /login returns 200', async () => {
      const response = await page.goto(url('login'), { waitUntil: 'domcontentloaded' });
      return { pass: response.status() === 200 };
    });

    await check('Registration creates an account and shows success', async () => {
      await register(page, 'Smoke Auth User', address);
      return { pass: (await page.locator('body').innerText()).includes('Account created. You can sign in now.') };
    });

    await check('Duplicate registration is rejected with an error', async () => {
      await register(page, 'Smoke Auth User', address);
      return { pass: (await page.locator('body').innerText()).includes('That email is already registered') };
    });

    await check('Wrong password shows the expected login error', async () => {
      await login(page, address, 'WrongPassword9!');
      return { pass: (await page.locator('body').innerText()).includes('Invalid email or password') };
    });

    await check('Valid login redirects to the user dashboard', async () => {
      await login(page, address);
      return { pass: new URL(page.url()).pathname.endsWith('/user/dashboard') };
    });

    await check('Dashboard renders all seeded goal types', async () => {
      const options = await page.locator('select[name="typeId"] option').allTextContents();
      return { pass: ['Study', 'Fitness', 'Career', 'Personal'].every(type => options.some(option => option.includes(type))) };
    });

    await check('A normal user receives HTTP 403 from the admin dashboard', async () => {
      const response = await page.goto(url('admin/dashboard'), { waitUntil: 'domcontentloaded' });
      return { pass: response.status() === 403 };
    });

    await check('Logout clears the session and protected routes return to login', async () => {
      await page.goto(url('logout'), { waitUntil: 'domcontentloaded' });
      const response = await page.goto(url('user/dashboard'), { waitUntil: 'domcontentloaded' });
      return {
        pass: response.status() === 200 &&
          new URL(page.url()).pathname.endsWith('/login')
      };
    });

    const guest = await newSession(browser);
    try {
      await check('Unauthenticated user and admin routes redirect to login', async () => {
        const userResponse = await guest.page.goto(url('user/dashboard'), { waitUntil: 'domcontentloaded' });
        const userRedirected = new URL(guest.page.url()).pathname.endsWith('/login');
        const adminResponse = await guest.page.goto(url('admin/dashboard'), { waitUntil: 'domcontentloaded' });
        return {
          pass: userResponse.status() === 200 && adminResponse.status() === 200 &&
            userRedirected && new URL(guest.page.url()).pathname.endsWith('/login')
        };
      });
    } finally {
      await guest.context.close();
    }
  } finally {
    await context.close();
    await browser.close();
  }
  return getFailures();
}

if (require.main === module) {
  run().then(failures => { process.exitCode = failures ? 1 : 0; })
    .catch(error => { console.error(`FAIL Auth flow: ${error.message.split('\n')[0]}`); process.exitCode = 1; });
}

module.exports = run;
