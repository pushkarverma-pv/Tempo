const {
  check, email, getFailures, login, newSession, openBrowser, password, rowFor, runId, url
} = require('./helpers.cjs');

async function run() {
  const browser = await openBrowser();
  const owner = await newSession(browser);
  let admin;
  try {
    await check('Registered user can still log in after the Tomcat restart', async () => {
      await login(owner.page, email('owner'));
      return { pass: new URL(owner.page.url()).pathname.endsWith('/user/dashboard') };
    });

    await check('Goal data is still present after the Tomcat restart', async () => {
      const body = await owner.page.locator('body').innerText();
      return { pass: body.includes(`Persistent goal ${runId}`) };
    });

    admin = await newSession(browser);
    await check('Admin login works after the Tomcat restart', async () => {
      await login(admin.page, 'admin@tempo.app', 'Admin@123');
      return { pass: new URL(admin.page.url()).pathname.endsWith('/admin/dashboard') };
    });

    await check('Test accounts, goals, and logs can be cleaned up after persistence verification', async () => {
      const goalsBefore = Number(await admin.page.locator('.kpis > div').nth(1).locator('b').innerText());
      const hoursBefore = Number((await admin.page.locator('.kpis > div').nth(3).locator('b').innerText()).replace(/[^\d.]/g, ''));
      for (const label of ['owner', 'other', 'auth']) {
        const address = email(label);
        const matching = admin.page.getByRole('row').filter({ hasText: address });
        if (await matching.count()) {
          const row = matching.last();
          admin.page.once('dialog', dialog => dialog.accept());
          await row.locator('form[action$="/admin/user/delete"] button').click();
        }
      }
      const goalsAfter = Number(await admin.page.locator('.kpis > div').nth(1).locator('b').innerText());
      const hoursAfter = Number((await admin.page.locator('.kpis > div').nth(3).locator('b').innerText()).replace(/[^\d.]/g, ''));
      const body = await admin.page.locator('body').innerText();
      const usersRemoved = ['owner', 'other', 'auth'].every(label => !body.includes(email(label)));
      const typeRow = rowFor(admin.page, `Smoke type ${runId}`);
      if (await typeRow.count()) {
        admin.page.once('dialog', dialog => dialog.accept());
        await typeRow.locator('form[action$="/admin/type/delete"] button').click();
      }
      return {
        pass: usersRemoved && goalsBefore - goalsAfter === 2 &&
          Math.round((hoursBefore - hoursAfter) * 10) === 5,
        detail: `accountsRemoved=${usersRemoved}, goals=${goalsBefore}->${goalsAfter}, hours=${hoursBefore}->${hoursAfter}`
      };
    });
  } finally {
    await owner.context.close();
    if (admin) await admin.context.close();
    await browser.close();
  }
  return getFailures();
}

if (require.main === module) {
  run().then(failures => { process.exitCode = failures ? 1 : 0; })
    .catch(error => { console.error(`FAIL Persistence check: ${error.message.split('\n')[0]}`); process.exitCode = 1; });
}

module.exports = run;
