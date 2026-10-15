const {
  addGoal, addLog, check, email, futureDate, getFailures, goalRowFor, localDateTime, login,
  newSession, openBrowser, password, register, rowFor, runId, url
} = require('./helpers.cjs');

async function createUser(browser, label) {
  const session = await newSession(browser);
  const address = email(label);
  await register(session.page, `Smoke ${label}`, address);
  await login(session.page, address);
  return { ...session, address };
}

async function readRowId(row, action) {
  return row.locator(`form[action$="${action}"] input[name="id"]`).inputValue();
}

async function run() {
  const browser = await openBrowser();
  let owner;
  let other;
  try {
    owner = await createUser(browser, 'owner');
    other = await createUser(browser, 'other');

    await check('A valid future-dated goal is created and appears on the dashboard', async () => {
      const study = owner.page.locator('select[name="typeId"] option').filter({ hasText: 'Study' }).first();
      const typeId = await study.getAttribute('value');
      await addGoal(owner.page, `Smoke goal ${runId}`, typeId, 1, futureDate());
      return { pass: (await owner.page.locator('body').innerText()).includes(`Smoke goal ${runId}`) };
    });

    await check('A past goal deadline is rejected', async () => {
      const typeId = await owner.page.locator('select[name="typeId"] option').first().getAttribute('value');
      await addGoal(owner.page, `Rejected past ${runId}`, typeId, 1, '2000-01-01');
      return { pass: (await owner.page.locator('body').innerText()).includes('Deadline cannot be in the past') };
    });

    await check('Target exceeding the selected goal type limit is rejected', async () => {
      const study = owner.page.locator('select[name="typeId"] option').filter({ hasText: 'Study' }).first();
      const typeId = await study.getAttribute('value');
      const maxHours = Number((await study.innerText()).match(/max\s+(\d+)h/i)[1]);
      await addGoal(owner.page, `Rejected cap ${runId}`, typeId, maxHours + 1, futureDate());
      return { pass: (await owner.page.locator('body').innerText()).includes('Target hours must be between') };
    });

    await check('A first 30-minute time log displays 50% progress', async () => {
      const mainRow = goalRowFor(owner.page, `Smoke goal ${runId}`);
      const goalId = await readRowId(mainRow, '/user/goal/delete');
      const start = new Date(Date.now() - 3 * 60 * 60 * 1000);
      const end = new Date(start.getTime() + 30 * 60 * 1000);
      owner.firstLog = localDateTime(start);
      owner.secondStart = localDateTime(end);
      owner.secondEnd = localDateTime(new Date(end.getTime() + 30 * 60 * 1000));
      owner.goalId = goalId;
      await addLog(owner.page, goalId, owner.firstLog, owner.secondStart);
      const rowText = await goalRowFor(owner.page, `Smoke goal ${runId}`).innerText();
      const flash = await owner.page.locator('.flash').allInnerTexts();
      return { pass: rowText.includes('50%'), detail: `dashboard row=${rowText}; notice=${flash.join(' | ') || 'none'}` };
    });

    await check('A second 30-minute log completes the one-hour goal', async () => {
      await addLog(owner.page, owner.goalId, owner.secondStart, owner.secondEnd);
      const row = goalRowFor(owner.page, `Smoke goal ${runId}`);
      const rowText = await row.innerText();
      const flash = await owner.page.locator('.flash').allInnerTexts();
      return {
        pass: rowText.includes('Completed') && rowText.includes('100%'),
        detail: `dashboard row=${rowText}; notice=${flash.join(' | ') || 'none'}`
      };
    });

    await check('Deleting one time log reopens the goal and restores 50% progress', async () => {
      const dateText = new Date(owner.firstLog).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric'
      });
      const startText = new Date(owner.firstLog).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
      const row = rowFor(owner.page, `${dateText}, ${startText}`);
      await row.locator('form[action$="/user/log/delete"] button').click();
      const goalRow = goalRowFor(owner.page, `Smoke goal ${runId}`);
      const rowText = await goalRow.innerText();
      return {
        pass: rowText.includes('50%') && !rowText.includes('Completed'),
        detail: `dashboard row=${rowText}`
      };
    });

    await check('Deleting the goal removes it and its remaining time log', async () => {
      const row = goalRowFor(owner.page, `Smoke goal ${runId}`);
      owner.page.once('dialog', dialog => dialog.accept());
      await row.locator('form[action$="/user/goal/delete"] button').click();
      const body = await owner.page.locator('body').innerText();
      return { pass: !body.includes(`Smoke goal ${runId}`) && !body.includes('0h 30m') };
    });

    await check('A different user can create a goal and a time log for ownership checks', async () => {
      const typeId = await other.page.locator('select[name="typeId"] option').first().getAttribute('value');
      other.goalName = `Other user goal ${runId}`;
      await addGoal(other.page, other.goalName, typeId, 1, futureDate());
      const row = rowFor(other.page, other.goalName);
      other.goalId = await readRowId(row, '/user/goal/delete');
      const start = new Date(Date.now() - 2 * 60 * 60 * 1000);
      const end = new Date(start.getTime() + 30 * 60 * 1000);
      other.logStart = localDateTime(start);
      await addLog(other.page, other.goalId, other.logStart, localDateTime(end));
      const logRow = rowFor(other.page, new Date(other.logStart).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric'
      }));
      other.logId = await readRowId(logRow, '/user/log/delete');
      return { pass: (await other.page.locator('body').innerText()).includes(other.goalName) };
    });

    await check('Unauthenticated requests redirect to login', async () => {
      const guest = await newSession(browser);
      try {
        await guest.page.goto(url('user/dashboard'), { waitUntil: 'domcontentloaded' });
        return { pass: new URL(guest.page.url()).pathname.endsWith('/login') };
      } finally {
        await guest.context.close();
      }
    });

    await check('A user cannot delete another user’s goal or time log', async () => {
      const goalResponse = await owner.context.request.post(url('user/goal/delete'), { form: { id: other.goalId } });
      const logResponse = await owner.context.request.post(url('user/log/delete'), { form: { id: other.logId } });
      await other.page.reload({ waitUntil: 'domcontentloaded' });
      const body = await other.page.locator('body').innerText();
      return {
        pass: goalResponse.status() === 200 && logResponse.status() === 200 &&
          body.includes(other.goalName) && body.includes('0h 30m')
      };
    });

    await check('The registered account and its goal are retained for restart persistence verification', async () => {
      const typeId = await owner.page.locator('select[name="typeId"] option').first().getAttribute('value');
      owner.persistGoal = `Persistent goal ${runId}`;
      await addGoal(owner.page, owner.persistGoal, typeId, 2, futureDate(10));
      return {
        pass: (await owner.page.locator('body').innerText()).includes(owner.persistGoal)
      };
    });
  } finally {
    if (owner) await owner.context.close();
    if (other) await other.context.close();
    await browser.close();
  }
  return getFailures();
}

if (require.main === module) {
  run().then(failures => { process.exitCode = failures ? 1 : 0; })
    .catch(error => { console.error(`FAIL Goal flow: ${error.message.split('\n')[0]}`); process.exitCode = 1; });
}

module.exports = run;
