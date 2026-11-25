import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from '../lib/discover.mjs';

const fixture = (name) => fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url));

test('discovers examples sorted by name with files in tab order', () => {
  const found = discoverExamples(fixture('basic'));
  assert.deepEqual(found.map(e => e.name), ['cases/create', 'cases/list']);
  assert.deepEqual(found[1].files.map(f => f.lang), ['python', 'java', 'cli']);
});
test('a directory with no recognised file fails', () => {
  assert.throws(() => discoverExamples(fixture('empty-example')), /cases\/empty/);
});
test('an unrecognised file in an example directory fails', () => {
  assert.throws(() => discoverExamples(fixture('stray-file')), /example\.py/);
});
