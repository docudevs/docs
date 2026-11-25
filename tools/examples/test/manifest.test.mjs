import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from '../lib/discover.mjs';
import { buildManifest } from '../lib/manifest.mjs';

const fixture = (name) => fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url));

test('manifest tabs carry fixed labels and prism languages', () => {
  const m = buildManifest(discoverExamples(fixture('basic')));
  assert.deepEqual(
    m['cases/create'].tabs.map((t) => [t.value, t.label, t.language]),
    [['curl', 'cURL', 'bash'], ['python', 'Python', 'python']],
  );
  assert.equal(typeof m['cases/create'].tabs[0].code, 'string');
});
test('tab code is the extracted snippet and tabs follow file order', () => {
  const examples = discoverExamples(fixture('basic'));
  const m = buildManifest(examples);
  assert.deepEqual(
    m['cases/list'].tabs.map((t) => t.value),
    ['python', 'java', 'cli'],
  );
  assert.equal(m['cases/list'].tabs[0].code, examples[1].files[0].snippet);
});
