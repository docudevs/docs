import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from '../lib/discover.mjs';
import { compareGaps, computeGaps, parseKnownGaps, parseNotApplicable, staleNotApplicable } from '../lib/coverage.mjs';

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

const pythonOnly = [{ name: 'a/b', files: [{ lang: 'python' }] }];

test('a pair listed as not applicable is no gap', () => {
  assert.deepEqual(computeGaps(pythonOnly, ['a/b:curl']), ['a/b:java']);
});
test('the same example without the line still reports the gap', () => {
  assert.deepEqual(computeGaps(pythonOnly, []), ['a/b:curl', 'a/b:java']);
});
test('parseNotApplicable strips reasons and comments', () => {
  assert.deepEqual(parseNotApplicable('# header\n\na/b:curl  # runs in the client\n'), ['a/b:curl']);
});
test('parseNotApplicable accepts java and rejects cli', () => {
  assert.deepEqual(parseNotApplicable('a/b:java  # the Java client has no method'), ['a/b:java']);
  assert.throws(() => parseNotApplicable('a/b:cli  # no'), /cli/);
});
test('parseNotApplicable requires a reason', () => {
  assert.throws(() => parseNotApplicable('a/b:curl'), /reason/);
});
test('staleNotApplicable reports a pair whose file exists', () => {
  const examples = [{ name: 'a/b', files: [{ lang: 'python' }, { lang: 'curl' }] }];
  assert.deepEqual(staleNotApplicable(examples, ['a/b:curl']), ['a/b:curl']);
  assert.deepEqual(staleNotApplicable(pythonOnly, ['a/b:curl']), []);
});
test('staleNotApplicable reports an unknown example', () => {
  assert.deepEqual(staleNotApplicable(pythonOnly, ['a/missing:curl']), ['a/missing:curl']);
});
