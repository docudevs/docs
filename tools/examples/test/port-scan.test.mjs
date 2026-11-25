import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { scanText } from '../../port/scan.mjs';

const SCAN = fileURLToPath(new URL('../../port/scan.mjs', import.meta.url));
const fence = (body) => `# Title\n\n\`\`\`bash\n${body}\n\`\`\`\n`;

test('a clean page has no findings', () => {
  const page = fence('curl -H "Authorization: Bearer $TOKEN" https://api.docudevs.ai/cases');
  assert.deepEqual(scanText(`${page}\nContact support@docudevs.ai.\n`, 'a.md', []), []);
});
test('each generic text pattern is reported with file and line', () => {
  for (const bad of ['/internal/', 'localhost', '127.0.0.1', 'azurecontainerapps.io', 'docudevs-doc', 'DOCUDEVS_API_KEY', 'Bearer $API_KEY']) {
    const findings = scanText(`line one\nsee ${bad} here\n`, 'a.md', []);
    assert.equal(findings.length, 1, bad);
    assert.match(findings[0], /^a\.md:2: /, bad);
  }
});
test('an API host other than api.docudevs.ai is a finding in a fence only', () => {
  assert.equal(scanText(fence('curl https://other.example.net/x'), 'a.md', []).length, 1);
  assert.deepEqual(scanText('See https://other.example.net/x for more.\n', 'a.md', []), []);
  assert.deepEqual(scanText(fence('curl https://api.docudevs.ai/x'), 'a.md', []), []);
});
test('non-markdown files are treated as code throughout', () => {
  assert.equal(scanText('url = "http://other.example.net/x"\n', 'a.py', []).length, 1);
});
test('emails are allowed at docudevs.ai, example.com and example.org only', () => {
  assert.deepEqual(scanText('a@docudevs.ai b@example.com c@example.org\n', 'a.md', []), []);
  assert.equal(scanText('mail someone@corp.test\n', 'a.md', []).length, 1);
});
test('a long hex or base64-looking token is a finding in a fence only', () => {
  const hex = 'a1b2c3d4e5f60718293a4b5c6d7e8f90';
  const b64 = 'Zm9vYmFyQmF6UXV4MTIzNDU2Nzg5MEFCQ0RFRg';
  assert.equal(scanText(fence(`key=${hex}`), 'a.md', []).length, 1);
  assert.equal(scanText(fence(`key=${b64}`), 'a.md', []).length, 1);
  assert.deepEqual(scanText(`prose ${hex}\n`, 'a.md', []), []);
  assert.deepEqual(scanText(fence('key=short1234'), 'a.md', []), []);
});
test('private literals are found anywhere and numbered without being echoed', () => {
  const findings = scanText('x\nuses SECRET-LITERAL-TWO inline\n', 'a.md', ['SECRET-LITERAL-ONE', 'SECRET-LITERAL-TWO']);
  assert.deepEqual(findings, ['a.md:2: forbidden literal #2']);
  assert.ok(!findings.join('').includes('SECRET-LITERAL'));
});

const run = (args, env, cwd) =>
  spawnSync('node', [SCAN, ...args], { env: { PATH: process.env.PATH, ...env }, cwd, encoding: 'utf8' });
const workspace = () => {
  const dir = mkdtempSync(join(tmpdir(), 'port-scan-'));
  writeFileSync(join(dir, 'literals.txt'), 'SECRET-LITERAL-ONE\n\nSECRET-LITERAL-TWO\n');
  return dir;
};

test('CLI exits 0 on clean files and 1 with findings, never echoing literals', () => {
  const dir = workspace();
  const env = { PORT_SCAN_LITERALS_FILE: join(dir, 'literals.txt') };
  mkdirSync(join(dir, 'docs'));
  writeFileSync(join(dir, 'docs', 'ok.md'), 'Clean page.\n');
  assert.equal(run([join(dir, 'docs')], env).status, 0);
  writeFileSync(join(dir, 'docs', 'bad.md'), 'a\nSECRET-LITERAL-TWO\n');
  const result = run([join(dir, 'docs')], env);
  assert.equal(result.status, 1);
  assert.match(result.stdout, /bad\.md:2: forbidden literal #2/);
  assert.ok(!(result.stdout + result.stderr).includes('SECRET-LITERAL'));
});
test('CLI exits 2 when the literals file is unset, missing or empty, or a path is missing', () => {
  const dir = workspace();
  writeFileSync(join(dir, 'page.md'), 'ok\n');
  writeFileSync(join(dir, 'empty.txt'), '\n\n');
  assert.equal(run([join(dir, 'page.md')], {}).status, 2);
  assert.equal(run([join(dir, 'page.md')], { PORT_SCAN_LITERALS_FILE: join(dir, 'nope.txt') }).status, 2);
  assert.equal(run([join(dir, 'page.md')], { PORT_SCAN_LITERALS_FILE: join(dir, 'empty.txt') }).status, 2);
  assert.equal(run([join(dir, 'missing.md')], { PORT_SCAN_LITERALS_FILE: join(dir, 'literals.txt') }).status, 2);
});
test('--diff scans only lines added since the base, by new line number', () => {
  const dir = workspace();
  const env = { PORT_SCAN_LITERALS_FILE: join(dir, 'literals.txt') };
  const git = (...args) => spawnSync('git', ['-c', 'user.name=t', '-c', 'user.email=t@example.com', ...args], { cwd: dir, encoding: 'utf8' });
  git('init', '-q', '-b', 'main');
  writeFileSync(join(dir, 'page.md'), 'localhost existed before\n');
  git('add', '.');
  git('commit', '-q', '-m', 'base');
  git('checkout', '-q', '-b', 'topic');
  writeFileSync(join(dir, 'page.md'), 'localhost existed before\nfine\nSECRET-LITERAL-ONE\n');
  git('commit', '-q', '-am', 'change');
  const result = run(['--diff', 'main'], env, dir);
  assert.equal(result.status, 1);
  assert.equal(result.stdout.trim(), 'page.md:3: forbidden literal #1');
});

test('findings never echo scanned text', () => {
  const findings = scanText(fence('curl https://private-host.corp.test/x'), 'a.md', []);
  assert.deepEqual(findings, ['a.md:4: non-API host']);
});
test('public hosts are allowed, other github.com paths are not', () => {
  const allowed = [
    'https://docs.docudevs.ai/x',
    'https://docudevs.ai',
    'http://example.com/a',
    'https://example.org',
    'https://json-schema.org/draft',
    'https://pypi.org/project/docudevs',
    'https://github.com/docudevs/docudevs-sdk',
  ];
  for (const url of allowed) assert.deepEqual(scanText(fence(`see ${url}`), 'a.md', []), [], url);
  assert.equal(scanText(fence('see https://github.com/someone/else'), 'a.md', []).length, 1);
  assert.equal(scanText(fence('see https://github.com/docudevs-evil/x'), 'a.md', []).length, 1);
});

const gitRepo = () => {
  const dir = workspace();
  const git = (...args) => spawnSync('git', ['-c', 'user.name=t', '-c', 'user.email=t@example.com', ...args], { cwd: dir, encoding: 'utf8' });
  git('init', '-q', '-b', 'main');
  writeFileSync(join(dir, 'seed.md'), 'seed\n');
  git('add', 'seed.md');
  git('commit', '-q', '-m', 'base');
  git('checkout', '-q', '-b', 'topic');
  return { dir, git, env: { PORT_SCAN_LITERALS_FILE: join(dir, 'literals.txt') } };
};
test('--diff skips the scanner and its test, and scans added lines starting with "++ "', () => {
  const { dir, git, env } = gitRepo();
  mkdirSync(join(dir, 'tools', 'port'), { recursive: true });
  mkdirSync(join(dir, 'tools', 'examples', 'test'), { recursive: true });
  writeFileSync(join(dir, 'tools', 'port', 'scan.mjs'), 'localhost\n');
  writeFileSync(join(dir, 'tools', 'examples', 'test', 'port-scan.test.mjs'), 'localhost\n');
  writeFileSync(join(dir, 'page.md'), '++ SECRET-LITERAL-ONE\n');
  git('add', 'tools', 'page.md');
  git('commit', '-q', '-m', 'change');
  const result = run(['--diff', 'main'], env, dir);
  assert.equal(result.status, 1);
  assert.equal(result.stdout.trim(), 'page.md:1: forbidden literal #1');
});
test('--diff exits 2 when git diff fails', () => {
  const { dir, env } = gitRepo();
  assert.equal(run(['--diff', 'no-such-ref'], env, dir).status, 2);
});
test('directory scans skip binary files with a note on stderr', () => {
  const dir = workspace();
  const env = { PORT_SCAN_LITERALS_FILE: join(dir, 'literals.txt') };
  mkdirSync(join(dir, 'd'));
  writeFileSync(join(dir, 'd', 'img.bin'), Buffer.from('SECRET-LITERAL-ONE\0\x01'));
  const result = run([join(dir, 'd')], env);
  assert.equal(result.status, 0);
  assert.match(result.stderr, /skipped binary .*img\.bin/);
});
