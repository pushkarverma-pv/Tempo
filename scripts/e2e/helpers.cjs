const { chromium } = require('playwright');

const configuredBaseUrl = process.env.TEMPO_BASE_URL || 'http://localhost:8080/tempo';
const baseUrl = new URL(configuredBaseUrl.endsWith('/') ? configuredBaseUrl : `${configuredBaseUrl}/`);
const runId = process.env.TEMPO_SMOKE_ID || Math.random().toString(36).slice(2, 10);
const password = process.env.TEMPO_SMOKE_PASSWORD || `TempoSmoke-${runId}!`;
let failures = 0;

function url(path) {
  return new URL(path.replace(/^\/+/, ''), baseUrl).toString();
}

function email(label) {
  return `tempo.${runId}.${label}@example.test`;
}

function report(name, passed, detail = '') {
  console.log(`${passed ? 'PASS' : 'FAIL'} ${name}${passed || !detail ? '' : `: ${detail}`}`);
  if (!passed) failures++;
  return passed;
}

async function check(name, action) {
  try {
    const result = await action();
    return report(name, result.pass, result.detail);
  } catch (error) {
    return report(name, false, error.message.split('\n')[0]);
  }
}

async function openBrowser() {
  return chromium.launch({ headless: true });
}

async function newSession(browser) {
  const context = await browser.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(12000);
  page.setDefaultNavigationTimeout(20000);
  return { context, page };
}

async function register(page, name, address) {
  await page.goto(url('register'), { waitUntil: 'domcontentloaded' });
  await page.locator('input[name="name"]').fill(name);
  await page.locator('input[name="email"]').fill(address);
  await page.locator('input[name="password"]').fill(password);
  await page.locator('input[name="confirm"]').fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();
}

async function login(page, address, secret = password) {
  await page.goto(url('login'), { waitUntil: 'domcontentloaded' });
  await page.locator('input[name="email"]').fill(address);
  await page.locator('input[name="password"]').fill(secret);
  await page.getByRole('button', { name: 'Sign in' }).click();
}

async function addGoal(page, name, typeId, targetHours, deadline) {
  const form = page.locator('form[action$="/user/goal/add"]');
  await form.locator('input[name="name"]').fill(name);
  await form.locator('select[name="typeId"]').selectOption(String(typeId));
  await form.locator('input[name="targetHours"]').fill(String(targetHours));
  await form.locator('input[name="deadline"]').fill(deadline);
  await form.getByRole('button', { name: 'Add goal' }).click();
}

async function addLog(page, goalId, start, end) {
  const form = page.locator('form[action$="/user/log/add"]');
  await form.locator('select[name="goalId"]').selectOption(String(goalId));
  await form.locator('input[name="start"]').fill(start);
  await form.locator('input[name="end"]').fill(end);
  await form.getByRole('button', { name: 'Save entry' }).click();
}

function futureDate(days = 5) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function localDateTime(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function rowFor(page, text) {
  return page.getByRole('row').filter({ hasText: text }).last();
}

function goalRowFor(page, text) {
  const section = page.locator('section.card').filter({
    has: page.getByRole('heading', { name: 'My goals', exact: true })
  });
  return section.getByRole('row').filter({ hasText: text }).last();
}

function resetFailures() {
  failures = 0;
}

function getFailures() {
  return failures;
}

module.exports = {
  addGoal,
  addLog,
  baseUrl,
  check,
  email,
  futureDate,
  goalRowFor,
  getFailures,
  localDateTime,
  login,
  newSession,
  openBrowser,
  password,
  register,
  report,
  resetFailures,
  rowFor,
  runId,
  url
};
