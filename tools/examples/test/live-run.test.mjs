import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
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
  formatWritesBlock,
  parseOrderFile,
  parseWritesFile,
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
test('parseOrderFile reads one example per line and ignores comments and blank lines', () => {
  assert.deepEqual(parseOrderFile('# header\n\ncases/list\njobs/delete-job\n', names), ['cases/list', 'jobs/delete-job']);
});
test('parseOrderFile rejects an example that does not exist', () => {
  assert.throws(() => parseOrderFile('cases/missing', names), /cases\/missing/);
});
test('parseOrderFile rejects an example listed twice', () => {
  assert.throws(() => parseOrderFile('cases/list\ncases/list', names), /twice/);
});
test('the committed order file is valid for the committed examples', () => {
  const found = discoverExamples(join(root, 'examples')).map((example) => example.name);
  const order = parseOrderFile(readFileSync(join(root, 'examples', 'live-order.txt'), 'utf8'), found);
  assert.ok(order.includes('configuration/save-configuration'));
  assert.ok(order.includes('templates/upload-template'));
});
test('selection runs the listed examples first in the listed order, then the rest alphabetically', () => {
  const runs = selectRuns({ examples, skip: new Map(), order: ['jobs/delete-job', 'cases/list'], variants: ['curl'] });
  assert.deepEqual(keys(runs), ['jobs/delete-job:curl', 'cases/list:curl', 'cases/create:curl']);
});
test('selection without an order keeps alphabetical order', () => {
  const runs = selectRuns({ examples, skip: new Map(), variants: ['curl'] });
  assert.deepEqual(keys(runs), ['cases/create:curl', 'cases/list:curl', 'jobs/delete-job:curl']);
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
    yes: false,
  });
  assert.equal(parseLiveArgs(['--yes']).yes, true);
  assert.deepEqual(parseLiveArgs([]), { only: undefined, variants: ['curl', 'python', 'cli'], list: false, yes: false });
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

test('parseWritesFile reads name, tab and description and rejects unknown examples', () => {
  const writes = parseWritesFile('# h\ncases/create\tconfiguration x\n', names);
  assert.equal(writes.get('cases/create'), 'configuration x');
  assert.throws(() => parseWritesFile('cases/missing\tconfiguration x', names), /cases\/missing/);
  assert.throws(() => parseWritesFile('cases/create', names), /what it writes/);
});
test('the committed writes file is valid for the committed examples', () => {
  const found = discoverExamples(join(root, 'examples')).map((example) => example.name);
  const writes = parseWritesFile(readFileSync(join(root, 'examples', 'live-writes.txt'), 'utf8'), found);
  assert.ok(writes.size > 0);
});
test('formatWritesBlock lists only what the selected examples write', () => {
  const writes = new Map([['cases/create', 'configuration x'], ['cases/list', 'template y']]);
  const block = formatWritesBlock(writes, ['cases/create', 'jobs/delete-job']);
  assert.match(block, /configuration x/);
  assert.doesNotMatch(block, /template y/);
});
test('redact replaces a key split across two output chunks once the chunks are joined', async () => {
  const fx = makeFixture({ 'a/split/curl.sh': `printf '%s' "${FAKE_KEY.slice(0, 8)}"\nsleep 0.3\nprintf '%s\\n' "${FAKE_KEY.slice(8)}"\n` });
  await run(fx, [pick('a/split', 'curl')]);
  const log = readFileSync(join(fx.outDir, 'a', 'split', 'curl.log'), 'utf8');
  assert.equal(log.includes(FAKE_KEY), false);
  assert.match(log, /\*\*\*/);
});
test('redact handles a key with regex-special characters', () => {
  const key = 'k.e*y+(1)[x]$^?|\\';
  assert.equal(redact(`a ${key} b`, key), 'a *** b');
});
test('the temporary working directory is removed after a run', async () => {
  const before = readdirSync(tmpdir()).filter((n) => n.startsWith('docudevs-live-')).length;
  const fx = makeFixture({ 'a/ok/curl.sh': 'echo hi\n' });
  await run(fx, [pick('a/ok', 'curl')]);
  assert.equal(readdirSync(tmpdir()).filter((n) => n.startsWith('docudevs-live-')).length, before);
});

const makeRoot = (files, extra = {}) => {
  const base = mkdtempSync(join(tmpdir(), 'live-root-'));
  const all = { 'live-skip.txt': '', 'live-order.txt': '', 'live-writes.txt': '', ...extra };
  for (const [path, body] of Object.entries(files)) {
    mkdirSync(dirname(join(base, 'examples', path)), { recursive: true });
    writeFileSync(join(base, 'examples', path), body);
  }
  for (const [path, body] of Object.entries(all)) writeFileSync(join(base, 'examples', path), body);
  mkdirSync(join(base, 'tools', 'examples', 'live-fixtures'), { recursive: true });
  return base;
};
const runIn = (base, args, extraEnv = {}) =>
  spawnSync('node', [script, ...args], {
    encoding: 'utf8',
    env: { PATH: process.env.PATH, HOME: process.env.HOME, LIVE_RUN_ROOT: base, API_KEY: FAKE_KEY, ...extraEnv },
  });

test('without --yes the script prints the writes block, exits 2 and runs nothing', () => {
  const base = makeRoot({ 'a/ok/curl.sh': 'touch ran\n' }, { 'live-writes.txt': 'a/ok\tconfiguration invoice-config\n' });
  const result = runIn(base, []);
  assert.equal(result.status, 2);
  assert.match(result.stdout + result.stderr, /configuration invoice-config/);
  assert.match(result.stdout + result.stderr, /re-run with --yes to run against this organization/);
  assert.equal(existsSync(join(base, 'live-run')), false);
});
test('--list needs no --yes', () => {
  const base = makeRoot({ 'a/ok/curl.sh': 'echo hi\n' });
  assert.equal(runIn(base, ['--list']).status, 0);
});
test('an empty selection exits 2', () => {
  const base = makeRoot({ 'a/ok/curl.sh': 'echo hi\n' });
  const result = runIn(base, ['--only', 'nothing', '--yes']);
  assert.equal(result.status, 2);
});
test('a missing venv exits 2 when a python variant is selected', () => {
  const base = makeRoot({ 'a/ok/python.py': 'echo hi\n' });
  assert.equal(runIn(base, ['--yes']).status, 2);
});
test('the script exits 1 when a row fails and 0 when all pass', () => {
  const bad = makeRoot({ 'a/bad/curl.sh': 'exit 3\n' });
  assert.equal(runIn(bad, ['--yes']).status, 1);
  const good = makeRoot({ 'a/ok/curl.sh': 'echo hi\n' });
  assert.equal(runIn(good, ['--yes']).status, 0);
});
test('the script redacts the key in the error line of an unexpected failure and exits 2', () => {
  const base = makeRoot({ 'a/ok/curl.sh': 'echo hi\n' }, { 'live-order.txt': `a/missing-${FAKE_KEY}\n` });
  const result = runIn(base, ['--yes']);
  assert.equal(result.status, 2);
  assert.equal((result.stdout + result.stderr).includes(FAKE_KEY), false);
});
test('SIGINT stops the child, removes the temp directory, writes the report and exits 130', async () => {
  const tmpRoot = mkdtempSync(join(tmpdir(), 'live-tmp-'));
  const pidFile = join(tmpRoot, 'child.pid');
  const base = makeRoot({ 'a/sleepy/curl.sh': `echo $$ > ${pidFile}\nsleep 30\n` });
  const child = spawn('node', [script, '--yes'], {
    env: { PATH: process.env.PATH, HOME: process.env.HOME, LIVE_RUN_ROOT: base, API_KEY: FAKE_KEY, TMPDIR: tmpRoot },
    stdio: 'ignore',
  });
  const exited = new Promise((resolve) => child.on('close', (code) => resolve(code)));
  const deadline = Date.now() + 10000;
  while (!existsSync(pidFile) && Date.now() < deadline) await new Promise((r) => setTimeout(r, 50));
  assert.ok(existsSync(pidFile));
  const pid = Number(readFileSync(pidFile, 'utf8').trim());
  child.kill('SIGINT');
  assert.equal(await exited, 130);
  await new Promise((r) => setTimeout(r, 200));
  assert.throws(() => process.kill(pid, 0));
  assert.deepEqual(readdirSync(tmpRoot).filter((n) => n.startsWith('docudevs-live-')), []);
  assert.ok(existsSync(join(base, 'live-run', 'report.md')));
});
