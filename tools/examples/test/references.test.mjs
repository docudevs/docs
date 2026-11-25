import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { findReferences } from '../lib/references.mjs';
import { assertReferencesResolve } from '../lib/manifest.mjs';

const fixture = (name) => fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url));

test('finds <Example name> in md and mdx, ignoring docs/openapi', () => {
  assert.deepEqual(findReferences(fixture('docs-tree')), [
    { page: 'guide.md', name: 'cases/create' },
    { page: 'nested/other.mdx', name: 'cases/list' },
  ]);
});
test('single-quoted and multi-line usages are found', () => {
  assert.deepEqual(findReferences(fixture('docs-multiline')), [{ page: 'page.mdx', name: 'cases/create' }]);
});
test('usages inside fenced code blocks are ignored', () => {
  assert.deepEqual(findReferences(fixture('docs-fenced')), [{ page: 'page.md', name: 'cases/list' }]);
});
test('unknown reference fails naming page and example', () => {
  assert.throws(
    () => assertReferencesResolve([{ page: 'guide.md', name: 'cases/creat' }], { 'cases/create': { tabs: [] } }),
    /guide\.md.*cases\/creat/s,
  );
});
test('known references pass', () => {
  assert.doesNotThrow(() =>
    assertReferencesResolve([{ page: 'guide.md', name: 'cases/create' }], { 'cases/create': { tabs: [] } }),
  );
});
