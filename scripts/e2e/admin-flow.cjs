const {
  addGoal, addLog, check, email, futureDate, getFailures, localDateTime, login,
  newSession, openBrowser, password, rowFor, runId, url
} = require('./helpers.cjs');

async function formSubmit(page, selector, values, button) {
  const form = page.locator(selector);
  for (const [name, value] of Object.entries(values)) {
    const input = form.locator(`[name="${name}"]`);
    if (await input.count()) {
      if (await input.evaluate(element => element.tagName.toLowerCase()) === 'select') {
        await input.selectOption(String(value));
      } else {
        await input.fill(String(value));
      }
    }
  }
  await form.getByRole('button', { name: button }).click();
}

async function run() {
  const browser = await openBrowser();
  const admin = await newSession(browser);
  let secondAdmin;
  let managedUser;
  try {
    await check('Default admin login redirects to /admin/dashboard', async () => {
      await login(admin.page, 'admin@tempo.app', 'Admin@123');
      return { pass: new URL(admin.page.url()).pathname.endsWith('/admin/dashboard') };
    });

    await check('Previous smoke-run accounts and goal types are cleaned up', async () => {
      const textRows = await admin.page.getByRole('row').allInnerTexts();
      const staleEmails = new Set();
      const staleTypes = new Set();
      for (const text of textRows) {
        const address = text.match(/tempo\.([a-f0-9]{10})\.(?:auth|owner|other|managed|self-admin)@example\.test/i);
        if (address && address[1].toLowerCase() !== runId.toLowerCase()) staleEmails.add(address[0]);
        const type = text.match(/Smoke type ([a-f0-9]{10})/i);
        if (type && type[1].toLowerCase() !== runId.toLowerCase()) staleTypes.add(type[0]);
      }
      for (const address of staleEmails) {
        const row = rowFor(admin.page, address);
        if (await row.count()) {
          admin.page.once('dialog', dialog => dialog.accept());
          await row.locator('form[action$="/admin/user/delete"] button').click();
        }
      }
      for (const name of staleTypes) {
        const row = rowFor(admin.page, name);
        if (await row.count()) {
          admin.page.once('dialog', dialog => dialog.accept());
          await row.locator('form[action$="/admin/type/delete"] button').click();
        }
      }
      return { pass: true };
    });

    await check('Admin dashboard shows usage figures and both chart canvases', async () => {
      const body = await admin.page.locator('body').innerText();
      const canvases = await admin.page.locator('canvas').count();
      return { pass: body.includes('accounts') && body.includes('goals set') && canvases === 2 };
    });

    const typeName = `Smoke type ${runId}`;
    await check('Admin can add a goal type and it appears in the table', async () => {
      await formSubmit(admin.page, 'form[action$="/admin/type/add"]', {
        name: typeName, metric: 'Smoke-test hours', maxHours: 20
      }, 'Save goal type');
      return { pass: (await admin.page.locator('body').innerText()).includes(typeName) };
    });

    await check('Duplicate goal type name is rejected with an error', async () => {
      await formSubmit(admin.page, 'form[action$="/admin/type/add"]', {
        name: typeName, metric: 'Duplicate', maxHours: 20
      }, 'Save goal type');
      return { pass: (await admin.page.locator('body').innerText()).includes('A goal type with that name already exists') };
    });

    const secondAdminEmail = email('self-admin');
    await check('Admin can create a second administrator account', async () => {
      await formSubmit(admin.page, 'form[action$="/admin/user/add"]', {
        name: `Smoke Self Admin ${runId}`, email: secondAdminEmail, password, role: 'ADMIN'
      }, 'Create account');
      return { pass: (await admin.page.locator('body').innerText()).includes(secondAdminEmail) };
    });

    secondAdmin = await newSession(browser);
    let secondAdminId;
    await check('The second administrator can sign in and cannot deactivate or delete their own account', async () => {
      const row = rowFor(admin.page, secondAdminEmail);
      secondAdminId = await row.locator('form[action$="/admin/user/toggle"] input[name="id"]').inputValue();
      await login(secondAdmin.page, secondAdminEmail);
      const toggle = await secondAdmin.page.evaluate(async ({ actionUrl, id }) => {
        const response = await fetch(actionUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ id }),
          credentials: 'same-origin'
        });
        return { status: response.status, body: await response.text() };
      }, { actionUrl: url('admin/user/toggle'), id: secondAdminId });
      const deleteResponse = await secondAdmin.page.evaluate(async ({ actionUrl, id }) => {
        const response = await fetch(actionUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ id }),
          credentials: 'same-origin'
        });
        return { status: response.status, body: await response.text() };
      }, { actionUrl: url('admin/user/delete'), id: secondAdminId });
      await secondAdmin.page.reload({ waitUntil: 'domcontentloaded' });
      const body = await secondAdmin.page.locator('body').innerText();
      const ownRow = await rowFor(secondAdmin.page, secondAdminEmail).innerText();
      const selfError = [toggle.body, deleteResponse.body, body].some(text => text.includes('You cannot do that to your own account'));
      return {
        pass: toggle.status === 200 && deleteResponse.status === 200 &&
          selfError &&
          body.includes(secondAdminEmail) && !ownRow.includes('Deactivated'),
        detail: `toggle=${toggle.status}, delete=${deleteResponse.status}, selfError=${selfError}, active=${!ownRow.includes('Deactivated')}`
      };
    });

    await check('Original admin can remove the temporary second admin afterwards', async () => {
      const row = rowFor(admin.page, secondAdminEmail);
      admin.page.once('dialog', dialog => dialog.accept());
      await row.locator('form[action$="/admin/user/delete"] button').click();
      return { pass: !(await admin.page.locator('body').innerText()).includes(secondAdminEmail) };
    });

    const managedEmail = email('managed');
    await check('Admin can create a user account', async () => {
      await formSubmit(admin.page, 'form[action$="/admin/user/add"]', {
        name: `Smoke Managed ${runId}`, email: managedEmail, password, role: 'USER'
      }, 'Create account');
      return { pass: (await admin.page.locator('body').innerText()).includes(managedEmail) };
    });

    managedUser = await newSession(browser);
    await check('Managed user can log in and create a goal with a time log', async () => {
      await login(managedUser.page, managedEmail);
      const typeId = await managedUser.page.locator('select[name="typeId"] option').first().getAttribute('value');
      const goalName = `Managed goal ${runId}`;
      await addGoal(managedUser.page, goalName, typeId, 1, futureDate());
      const goalId = await rowFor(managedUser.page, goalName)
        .locator('form[action$="/user/goal/delete"] input[name="id"]').inputValue();
      const start = new Date(Date.now() - 90 * 60 * 1000);
      await addLog(managedUser.page, goalId, localDateTime(start), localDateTime(new Date(start.getTime() + 30 * 60 * 1000)));
      await managedUser.page.goto(url('logout'), { waitUntil: 'domcontentloaded' });
      return { pass: (await admin.page.locator('body').innerText()).includes(managedEmail) };
    });

    await check('Admin deactivation marks the account and prevents login', async () => {
      const row = rowFor(admin.page, managedEmail);
      await row.locator('form[action$="/admin/user/toggle"]').getByRole('button').click();
      const deactivated = (await rowFor(admin.page, managedEmail).innerText()).includes('Deactivated');
      const loginPage = await newSession(browser);
      try {
        await login(loginPage.page, managedEmail);
        const denied = (await loginPage.page.locator('body').innerText()).includes('This account has been deactivated');
        return { pass: deactivated && denied };
      } finally {
        await loginPage.context.close();
      }
    });

    await check('Admin deletion removes the account, goal, and logged hours', async () => {
      const goalCount = Number(await admin.page.locator('.kpis > div').nth(1).locator('b').innerText());
      const hoursText = await admin.page.locator('.kpis > div').nth(3).locator('b').innerText();
      const hours = Number(hoursText.replace(/[^\d.]/g, ''));
      const row = rowFor(admin.page, managedEmail);
      admin.page.once('dialog', dialog => dialog.accept());
      await row.locator('form[action$="/admin/user/delete"] button').click();
      const afterGoalCount = Number(await admin.page.locator('.kpis > div').nth(1).locator('b').innerText());
      const afterHoursText = await admin.page.locator('.kpis > div').nth(3).locator('b').innerText();
      const afterHours = Number(afterHoursText.replace(/[^\d.]/g, ''));
      const absent = !(await admin.page.locator('body').innerText()).includes(managedEmail);
      const loginAttempt = await newSession(browser);
      try {
        await login(loginAttempt.page, managedEmail);
        const rejected = (await loginAttempt.page.locator('body').innerText()).includes('Invalid email or password');
        return { pass: absent && rejected && afterGoalCount === goalCount - 1 && afterHours === Math.round((hours - 0.5) * 10) / 10 };
      } finally {
        await loginAttempt.context.close();
      }
    });
  } finally {
    if (managedUser) await managedUser.context.close();
    if (secondAdmin) await secondAdmin.context.close();
    await admin.context.close();
    await browser.close();
  }
  return getFailures();
}

if (require.main === module) {
  run().then(failures => { process.exitCode = failures ? 1 : 0; })
    .catch(error => { console.error(`FAIL Admin flow: ${error.message.split('\n')[0]}`); process.exitCode = 1; });
}

module.exports = run;
