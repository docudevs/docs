import test from 'node:test';
import assert from 'node:assert/strict';
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from '../lib/discover.mjs';
import {
  ALLOWED_SKIP_REASONS,
  detectFixtureReads,
  executeRuns,
  formatReport,
  parseLiveArgs,
  parseSkipFile,
  redact,
  runExitCode,
  selectRuns,
} from '../lib/live-run.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
const FAKE_KEY = 'fake-key-0123456789';

const names = ['cases/create', 'cases/list', 'jobs/delete-job'];
const examples = names.map((name) => ({
  name,
  files: [{ lang: 'curl' }, { lang: 'python' }, { lang: 'cli' }, { lang: 'java' }],
}));
const partial = [{ name: 'a/only-python', files: [{ lang: 'python' }] }];

const keys = (runs) => runs.map((run) => `${run.name}:${run.variant}${run.skip ? ':skip' : ''}`);

test('parseSkipFile reads name, tab and reason and ignores comments', () => {
  const skip = parseSkipFile(`# header\n\ncases/create\tneeds a signed-in user\n`, names);
  assert.equal(skip.get('cases/create'), 'needs a signed-in user');
});
test('parseSkipFile rejects an example that does not exist', () => {
  assert.throws(() => parseSkipFile('cases/missing\tneeds a signed-in user', names), /cases\/missing/);
});
test('parseSkipFile rejects a line without a reason', () => {
  assert.throws(() => parseSkipFile('cases/create', names), /reason/);
  assert.throws(() => parseSkipFile('cases/create\t  ', names), /reason/);
});
test('parseSkipFile rejects a reason outside the allowed list', () => {
  assert.throws(() => parseSkipFile('cases/create\tbecause I said so', names), /allowed/);
});
test('the allowed reasons are exactly the six agreed ones', () => {
  assert.deepEqual(ALLOWED_SKIP_REASONS, [
    'needs a signed-in user',
    'needs a dedicated deployment',
    'needs a public callback URL',
    'needs provider credentials',
    'deletes or changes organization-wide settings',
    'needs an existing resource id',
  ]);
});
test('the committed skip file is valid for the committed examples', () => {
  const found = discoverExamples(join(root, 'examples')).map((example) => example.name);
  const skip = parseSkipFile(readFileSync(join(root, 'examples', 'live-skip.txt'), 'utf8'), found);
  assert.ok(skip.size > 0);
});

test('selection covers curl, python and cli for every example without java', () => {
  const runs = selectRuns({ examples, skip: new Map() });
  assert.equal(runs.length, 9);
  assert.ok(runs.every((run) => run.variant !== 'java'));
  assert.deepEqual(keys(runs).slice(0, 3), ['cases/create:curl', 'cases/create:python', 'cases/create:cli']);
});
test('selection skips only languages the example has', () => {
  assert.deepEqual(keys(selectRuns({ examples: partial, skip: new Map() })), ['a/only-python:python']);
});
test('selection honours --variant', () => {
  const runs = selectRuns({ examples, skip: new Map(), variants: ['python'] });
  assert.deepEqual(new Set(runs.map((run) => run.variant)), new Set(['python']));
  assert.equal(runs.length, 3);
});
test('selection honours --only with an area and with an area/name', () => {
  assert.deepEqual(new Set(selectRuns({ examples, skip: new Map(), only: 'cases' }).map((run) => run.name)), new Set(['cases/create', 'cases/list']));
  assert.deepEqual(new Set(selectRuns({ examples, skip: new Map(), only: 'cases/list' }).map((run) => run.name)), new Set(['cases/list']));
  assert.deepEqual(selectRuns({ examples, skip: new Map(), only: 'cas' }), []);
});
test('selection marks skip-file examples as skipped with the reason and never selects them to run', () => {
  const skip = new Map([['jobs/delete-job', 'deletes or changes organization-wide settings']]);
  const runs = selectRuns({ examples, skip });
  const skipped = runs.filter((run) => run.skip);
  assert.equal(skipped.length, 3);
  assert.ok(skipped.every((run) => run.name === 'jobs/delete-job' && run.skip === 'deletes or changes organization-wide settings'));
  assert.equal(runs.filter((run) => !run.skip).length, 6);
});
test('--only does not override the skip file', () => {
  const skip = new Map([['jobs/delete-job', 'needs a signed-in user']]);
  const runs = selectRuns({ examples, skip, only: 'jobs/delete-job' });
  assert.ok(runs.every((run) => run.skip));
});

test('parseLiveArgs reads flags', () => {
  assert.deepEqual(parseLiveArgs(['--only', 'cases/list', '--variant', 'curl', '--list']), {
    only: 'cases/list',
    variants: ['curl'],
    list: true,
  });
  assert.deepEqual(parseLiveArgs([]), { only: undefined, variants: ['curl', 'python', 'cli'], list: false });
});
test('parseLiveArgs rejects an unknown variant, flag or missing value', () => {
  assert.throws(() => parseLiveArgs(['--variant', 'java']), /variant/);
  assert.throws(() => parseLiveArgs(['--nope']), /--nope/);
  assert.throws(() => parseLiveArgs(['--only']), /--only/);
});

test('detectFixtureReads finds quoted file names opened or uploaded', () => {
  assert.deepEqual(detectFixtureReads('with open("invoice.pdf", "rb") as f:\n  data = f.read()'), ['invoice.pdf']);
  assert.deepEqual(detectFixtureReads('curl -F "document=@contract.pdf" -F "x=1"'), ['contract.pdf']);
  assert.deepEqual(detectFixtureReads("client.submit(document=open('a.docx','rb'))"), ['a.docx']);
});
test('detectFixtureReads ignores names the example writes', () => {
  assert.deepEqual(detectFixtureReads('with open("filled.docx", "wb") as f:\n  f.write(b)'), []);
  assert.deepEqual(detectFixtureReads('curl -o result.xlsx "https://api.docudevs.ai/x"'), []);
  assert.deepEqual(detectFixtureReads('curl --output result.csv "https://api.docudevs.ai/x"'), []);
  assert.deepEqual(detectFixtureReads('echo "$X" > config.json'), []);
  assert.deepEqual(detectFixtureReads('await client.compare(save_to="redline.pdf")'), []);
});
test('detectFixtureReads ignores names on the output allowlist', () => {
  assert.deepEqual(detectFixtureReads('print(Path("response.json").read_text())', ['response.json']), []);
});
test('detectFixtureReads ignores unknown extensions, URLs and duplicates', () => {
  assert.deepEqual(detectFixtureReads('x = "notes.rtf"'), []);
  assert.deepEqual(detectFixtureReads('curl "https://api.docudevs.ai/spec.json"'), []);
  assert.deepEqual(detectFixtureReads('open("a.pdf"); open("a.pdf"); open("b.png")'), ['a.pdf', 'b.png']);
});
test('every file an unskipped example reads has a fixture', () => {
  const found = discoverExamples(join(root, 'examples'));
  const skip = parseSkipFile(readFileSync(join(root, 'examples', 'live-skip.txt'), 'utf8'), found.map((e) => e.name));
  const fixtures = new Set(readdirSync(join(root, 'tools', 'examples', 'live-fixtures')));
  const missing = [];
  for (const example of found) {
    if (skip.has(example.name)) continue;
    for (const file of example.files.filter((f) => f.lang !== 'java')) {
      for (const read of detectFixtureReads(file.source)) {
        if (!fixtures.has(read)) missing.push(`${file.file}: ${read}`);
      }
    }
  }
  assert.deepEqual(missing, []);
});

test('redact replaces every occurrence of the key', () => {
  assert.equal(redact(`a ${FAKE_KEY} b ${FAKE_KEY}`, FAKE_KEY), 'a *** b ***');
  assert.equal(redact('nothing here', FAKE_KEY), 'nothing here');
});
test('redact does nothing without a key', () => {
  assert.equal(redact('abc', ''), 'abc');
});

const makeFixture = (files) => {
  const base = mkdtempSync(join(tmpdir(), 'live-run-test-'));
  const examplesDir = join(base, 'examples');
  const fixturesDir = join(base, 'fixtures');
  const outDir = join(base, 'live-run');
  mkdirSync(fixturesDir, { recursive: true });
  writeFileSync(join(fixturesDir, 'invoice.pdf'), 'PDF');
  for (const [path, body] of Object.entries(files)) {
    mkdirSync(dirname(join(examplesDir, path)), { recursive: true });
    writeFileSync(join(examplesDir, path), body);
  }
  const fakeBin = join(base, 'venv-bin');
  mkdirSync(fakeBin);
  writeFileSync(join(fakeBin, 'python'), '#!/bin/sh\nexec bash "$@"\n');
  chmodSync(join(fakeBin, 'python'), 0o755);
  writeFileSync(join(fakeBin, 'docudevs'), '#!/bin/sh\necho cli-stub\n');
  chmodSync(join(fakeBin, 'docudevs'), 0o755);
  return { base, examplesDir, fixturesDir, outDir, venvBin: join(fakeBin) };
};

const run = (fx, runs, extra = {}) =>
  executeRuns({
    runs,
    examplesDir: fx.examplesDir,
    fixturesDir: fx.fixturesDir,
    outDir: fx.outDir,
    venvBin: fx.venvBin,
    apiKey: FAKE_KEY,
    apiUrl: 'https://api.docudevs.ai',
    parentEnv: { PATH: process.env.PATH, HOME: process.env.HOME, SECRET_OTHER: 'leak', API_KEY: FAKE_KEY },
    timeoutMs: 5000,
    log: () => {},
    ...extra,
  });

const pick = (name, variant, skip = null) => ({ name, variant, skip });

test('a passing variant is recorded as pass and logged', async () => {
  const fx = makeFixture({ 'a/ok/curl.sh': 'echo hello\n' });
  const [result] = await run(fx, [pick('a/ok', 'curl')]);
  assert.equal(result.status, 'pass');
  assert.match(readFileSync(join(fx.outDir, 'a', 'ok', 'curl.log'), 'utf8'), /hello/);
});
test('a failing variant records the exit code and last output line', async () => {
  const fx = makeFixture({ 'a/bad/curl.sh': 'echo first\necho "the last line"\nexit 3\n' });
  const [result] = await run(fx, [pick('a/bad', 'curl')]);
  assert.equal(result.status, 'fail');
  assert.equal(result.exitCode, 3);
  assert.equal(result.lastLine, 'the last line');
});
test('a variant that exceeds the timeout fails with a timeout row', async () => {
  const fx = makeFixture({ 'a/slow/curl.sh': 'echo started\nsleep 30\n' });
  const started = Date.now();
  const [result] = await run(fx, [pick('a/slow', 'curl')], { timeoutMs: 300 });
  assert.ok(Date.now() - started < 2000);
  assert.equal(result.status, 'fail');
  assert.equal(result.timedOut, true);
  assert.match(formatReport([result]), /timed out/);
});
test('skipped runs are recorded with their reason and never executed', async () => {
  const fx = makeFixture({ 'a/del/curl.sh': 'touch ran.marker\nexit 9\n' });
  const [result] = await run(fx, [pick('a/del', 'curl', 'needs a signed-in user')]);
  assert.equal(result.status, 'skipped');
  assert.equal(result.reason, 'needs a signed-in user');
  assert.equal(existsSync(join(fx.outDir, 'a', 'del', 'curl.log')), false);
});
test('the working directory holds the fixtures and the python and cli variants run', async () => {
  const fx = makeFixture({
    'a/files/curl.sh': 'test -f invoice.pdf && echo found\n',
    'a/files/python.py': 'test -f invoice.pdf && echo py-found\n',
    'a/files/cli.sh': 'docudevs go\n',
  });
  const results = await run(fx, [pick('a/files', 'curl'), pick('a/files', 'python'), pick('a/files', 'cli')]);
  assert.deepEqual(results.map((r) => r.status), ['pass', 'pass', 'pass']);
  assert.match(readFileSync(join(fx.outDir, 'a', 'files', 'python.log'), 'utf8'), /py-found/);
  assert.match(readFileSync(join(fx.outDir, 'a', 'files', 'cli.log'), 'utf8'), /cli-stub/);
});
test('the child environment holds only the allowed variables', async () => {
  const fx = makeFixture({ 'a/env/curl.sh': 'env | cut -d= -f1 | sort\n' });
  await run(fx, [pick('a/env', 'curl')]);
  const names = readFileSync(join(fx.outDir, 'a', 'env', 'curl.log'), 'utf8').split('\n').filter(Boolean);
  assert.ok(!names.includes('SECRET_OTHER'));
  for (const allowed of ['API_KEY', 'PATH', 'HOME', 'DOCUDEVS_API_URL']) assert.ok(names.includes(allowed), allowed);
});
test('the key is replaced with *** in logs, rows and the report', async () => {
  const fx = makeFixture({
    'a/leak/curl.sh': 'echo "token $API_KEY here"\necho "last $API_KEY"\nexit 1\n',
  });
  const results = await run(fx, [pick('a/leak', 'curl')]);
  const log = readFileSync(join(fx.outDir, 'a', 'leak', 'curl.log'), 'utf8');
  assert.ok(!log.includes(FAKE_KEY));
  assert.match(log, /token \*\*\* here/);
  assert.equal(results[0].lastLine, 'last ***');
  assert.ok(!formatReport(results).includes(FAKE_KEY));
});
test('the key never appears in any file written by a run', async () => {
  const fx = makeFixture({ 'a/leak/curl.sh': 'echo "$API_KEY"\n' });
  await run(fx, [pick('a/leak', 'curl')]);
  const walk = (dir) =>
    readdirSync(dir, { withFileTypes: true }).flatMap((entry) =>
      entry.isDirectory() ? walk(join(dir, entry.name)) : [join(dir, entry.name)],
    );
  for (const file of walk(fx.outDir)) assert.ok(!readFileSync(file, 'utf8').includes(FAKE_KEY), file);
});

const sample = [
  { name: 'a/ok', variant: 'curl', status: 'pass' },
  { name: 'a/bad', variant: 'python', status: 'fail', exitCode: 3, lastLine: 'boom | pipe' },
  { name: 'a/slow', variant: 'cli', status: 'fail', timedOut: true, exitCode: null, lastLine: 'started' },
  { name: 'a/del', variant: 'curl', status: 'skipped', reason: 'needs a signed-in user' },
];
test('the report has one row per example and variant', () => {
  const report = formatReport(sample);
  const rows = report.split('\n').filter((line) => line.startsWith('| a/'));
  assert.equal(rows.length, 4);
  assert.match(rows[0], /a\/ok \| curl \| pass/);
  assert.match(rows[1], /fail \(exit 3\): boom \\\| pipe/);
  assert.match(rows[2], /fail \(timed out\)/);
  assert.match(rows[3], /skipped \(needs a signed-in user\)/);
});
test('exit code is 1 when any row failed and 0 otherwise', () => {
  assert.equal(runExitCode(sample), 1);
  assert.equal(runExitCode(sample.filter((r) => r.status !== 'fail')), 0);
  assert.equal(runExitCode([]), 0);
});

const script = join(root, 'tools', 'examples', 'run-live.mjs');
const runScript = async (args, env) => {
  const { spawnSync } = await import('node:child_process');
  return spawnSync(process.execPath, [script, ...args], { env: { PATH: process.env.PATH, ...env }, encoding: 'utf8' });
};
test('the script exits 2 with a one-line message when API_KEY is unset or empty', async () => {
  for (const env of [{}, { API_KEY: '' }]) {
    const result = await runScript([], env);
    assert.equal(result.status, 2);
    assert.equal(result.stderr.trim().split('\n').length, 1);
    assert.match(result.stderr, /API_KEY/);
  }
});
test('--list prints the selection without running anything and without the key', async () => {
  const result = await runScript(['--list', '--only', 'quick-start'], { API_KEY: FAKE_KEY });
  assert.equal(result.status, 0);
  assert.match(result.stdout, /quick-start\//);
  assert.ok(!result.stdout.includes(FAKE_KEY));
});
test('the script exits 2 on a bad flag', async () => {
  const result = await runScript(['--variant', 'java'], { API_KEY: FAKE_KEY });
  assert.equal(result.status, 2);
});
