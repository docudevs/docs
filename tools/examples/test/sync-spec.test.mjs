import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';
import { checkOwnedFiles, syncOwnedFiles, syncSpec } from '../sync-spec.mjs';

const script = join(dirname(fileURLToPath(import.meta.url)), '..', 'sync-spec.mjs');

function workspace() {
  const dir = mkdtempSync(join(tmpdir(), 'sync-spec-'));
  const mainRoot = join(dir, 'main');
  mkdirSync(mainRoot);
  return { mainRoot, destination: join(dir, 'static', 'docudevs.yaml'), dir };
}

test('copies the spec and reports its line count when the main checkout exists', () => {
  const { mainRoot, destination } = workspace();
  writeFileSync(join(mainRoot, 'docudevs.yaml'), 'openapi: 3.1.0\ninfo: {}\npaths: {}\n');

  assert.deepEqual(syncSpec({ mainRoot, destination }), { copied: true, lines: 3 });
  assert.equal(readFileSync(destination, 'utf8'), 'openapi: 3.1.0\ninfo: {}\npaths: {}\n');
});

test('overwrites an existing vendored spec', () => {
  const { mainRoot, destination, dir } = workspace();
  mkdirSync(join(dir, 'static'));
  writeFileSync(destination, 'old\n');
  writeFileSync(join(mainRoot, 'docudevs.yaml'), 'new\n');

  assert.equal(syncSpec({ mainRoot, destination }).copied, true);
  assert.equal(readFileSync(destination, 'utf8'), 'new\n');
});

test('keeps the vendored spec when the main checkout has no spec', () => {
  const { destination, dir } = workspace();
  mkdirSync(join(dir, 'static'));
  writeFileSync(destination, 'vendored\n');

  assert.deepEqual(syncSpec({ mainRoot: join(dir, 'missing'), destination }), { copied: false });
  assert.equal(readFileSync(destination, 'utf8'), 'vendored\n');
});

function ownedWorkspace() {
  const dir = mkdtempSync(join(tmpdir(), 'owned-'));
  const mainRoot = join(dir, 'main');
  const docsRoot = join(dir, 'docs-repo');
  mkdirSync(join(mainRoot, 'docudevs-sdk', 'skills', 'playbook-evaluation'), { recursive: true });
  mkdirSync(join(docsRoot, 'docs', 'core'), { recursive: true });
  mkdirSync(join(docsRoot, 'docs', 'reference'), { recursive: true });
  mkdirSync(join(docsRoot, 'static', 'files'), { recursive: true });
  writeFileSync(join(mainRoot, 'docudevs.yaml'), 'openapi: 3.1.0\n');
  writeFileSync(join(mainRoot, 'pipeline.schema.json'), '{"a":1}\n');
  writeFileSync(
    join(mainRoot, 'docudevs-sdk', 'skills', 'playbook-evaluation', 'SKILL.md'),
    '---\nname: skill\ndescription: skill front matter\n---\n\n# Body\n\nNew text.\n',
  );
  writeFileSync(
    join(docsRoot, 'docs', 'core', 'playbook-evaluation.md'),
    '---\ntitle: Page title\ndescription: Page description\n---\n\n# Body\n\nOld text.\n',
  );
  return { mainRoot, docsRoot, dir };
}

const page = (docsRoot) => join(docsRoot, 'docs', 'core', 'playbook-evaluation.md');

test('syncOwnedFiles copies the spec, the schema and the guide body', () => {
  const { mainRoot, docsRoot } = ownedWorkspace();

  const { copied } = syncOwnedFiles({ mainRoot, docsRoot });

  assert.deepEqual(copied.sort(), [
    'docs/core/playbook-evaluation.md',
    'docs/reference/pipeline.schema.json',
    'static/files/docudevs.yaml',
  ]);
  assert.equal(readFileSync(join(docsRoot, 'static', 'files', 'docudevs.yaml'), 'utf8'), 'openapi: 3.1.0\n');
  assert.equal(readFileSync(join(docsRoot, 'docs', 'reference', 'pipeline.schema.json'), 'utf8'), '{"a":1}\n');
});

test('syncOwnedFiles keeps the page front matter and replaces only its body', () => {
  const { mainRoot, docsRoot } = ownedWorkspace();

  syncOwnedFiles({ mainRoot, docsRoot });

  assert.equal(
    readFileSync(page(docsRoot), 'utf8'),
    '---\ntitle: Page title\ndescription: Page description\n---\n\n# Body\n\nNew text.\n',
  );
});

test('syncOwnedFiles skips the schema when the main checkout root has none', () => {
  const { mainRoot, docsRoot } = ownedWorkspace();
  rmSync(join(mainRoot, 'pipeline.schema.json'));

  const { copied } = syncOwnedFiles({ mainRoot, docsRoot });

  assert.deepEqual(copied.sort(), ['docs/core/playbook-evaluation.md', 'static/files/docudevs.yaml']);
  assert.equal(existsSync(join(docsRoot, 'docs', 'reference', 'pipeline.schema.json')), false);
});

test('checkOwnedFiles reports the guide page when it is missing from the docs', () => {
  const { mainRoot, docsRoot } = ownedWorkspace();
  syncOwnedFiles({ mainRoot, docsRoot });
  rmSync(page(docsRoot));

  assert.deepEqual(checkOwnedFiles({ mainRoot, docsRoot }), ['docs/core/playbook-evaluation.md']);
});

test('checkOwnedFiles reports nothing once synced', () => {
  const { mainRoot, docsRoot } = ownedWorkspace();
  syncOwnedFiles({ mainRoot, docsRoot });

  assert.deepEqual(checkOwnedFiles({ mainRoot, docsRoot }), []);
});

test('checkOwnedFiles reports a changed guide body and a changed schema', () => {
  const { mainRoot, docsRoot } = ownedWorkspace();
  syncOwnedFiles({ mainRoot, docsRoot });
  writeFileSync(
    join(mainRoot, 'docudevs-sdk', 'skills', 'playbook-evaluation', 'SKILL.md'),
    '---\nname: skill\n---\n\n# Body\n\nNewer text.\n',
  );
  writeFileSync(join(mainRoot, 'pipeline.schema.json'), '{"a":2}\n');

  assert.deepEqual(checkOwnedFiles({ mainRoot, docsRoot }).sort(), [
    'docs/core/playbook-evaluation.md',
    'docs/reference/pipeline.schema.json',
  ]);
});

test('the check command succeeds with a message when no main checkout exists', () => {
  const { dir } = ownedWorkspace();
  const result = spawnSync(process.execPath, [script, '--check'], {
    env: { ...process.env, DOCUDEVS_MAIN_ROOT: join(dir, 'missing') },
    encoding: 'utf8',
  });

  assert.equal(result.status, 0);
  assert.match(result.stdout, /No main checkout found/);
});

test('the check command exits 1 naming differing files and never printing contents', () => {
  const { mainRoot } = ownedWorkspace();
  writeFileSync(join(mainRoot, 'pipeline.schema.json'), '{"secret-content":1}\n');
  const result = spawnSync(process.execPath, [script, '--check'], {
    env: { ...process.env, DOCUDEVS_MAIN_ROOT: mainRoot },
    encoding: 'utf8',
  });

  assert.equal(result.status, 1);
  assert.match(result.stdout, /docs\/reference\/pipeline\.schema\.json/);
  assert.doesNotMatch(result.stdout + result.stderr, /secret-content/);
});
