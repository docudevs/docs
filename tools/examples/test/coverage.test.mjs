import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from '../lib/discover.mjs';
import { compareGaps, computeGaps, parseKnownGaps } from '../lib/coverage.mjs';

const fixture = (name) => fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url));

test('gaps list missing guaranteed languages only', () => {
  assert.deepEqual(computeGaps(discoverExamples(fixture('basic'))), ['cases/create:java', 'cases/list:curl']);
});
test('ratchet reports unlisted and stale entries', () => {
  assert.deepEqual(compareGaps(['a/b:java', 'a/c:curl'], ['a/b:java', 'a/d:python']), {
    unlisted: ['a/c:curl'],
    stale: ['a/d:python'],
  });
});
test('parseKnownGaps ignores blank and comment lines', () => {
  assert.deepEqual(parseKnownGaps('# heading\n\na/b:java\n  \n# more\na/c:curl\n'), ['a/b:java', 'a/c:curl']);
});
