import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { syncSpec } from '../sync-spec.mjs';

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
