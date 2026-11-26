import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync, existsSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { extractSnippet } from '../lib/snippet.mjs';
import { generateFlatDocs, runFlatDocs } from '../lib/flat-docs.mjs';

const script = join(dirname(fileURLToPath(import.meta.url)), '..', 'build-flat-docs.mjs');
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');

const CURL = '#!/bin/bash\nAPI_KEY=x\n# docs:start\ncurl -f "https://api.docudevs.ai/a"\n# docs:end\n';
const PYTHON = 'import asyncio\n# docs:start\nawait client.a()\n# docs:end\n';
const JAVA = 'package examples.cases.list;\npublic class Example {\n  void m() {\n    // docs:start\n    client.a();\n    // docs:end\n  }\n}\n';
const CLI = 'docudevs a\n';

function write(root, path, content) {
  mkdirSync(dirname(join(root, path)), { recursive: true });
  writeFileSync(join(root, path), content);
}

function fixture(pages = {}) {
  const root = mkdtempSync(join(tmpdir(), 'flat-docs-'));
  write(root, 'examples/cases/list/curl.sh', CURL);
  write(root, 'examples/cases/list/python.py', PYTHON);
  write(root, 'examples/cases/list/Example.java', JAVA);
  write(root, 'examples/cases/list/cli.sh', CLI);
  write(root, 'examples/cases/only-curl/curl.sh', 'curl x\n');
  const defaults = {
    'docs/intro.md': "---\ntitle: Intro\ndescription: D\n---\n\nimport Foo from '@theme/Foo';\n\n# Intro\n\n<Example name=\"cases/list\" />\n\nSee [guide](/docs/guide) and [sibling](./guide.md).\n",
    'docs/openapi/api.md': '# generated\n<Example name="nope/none" />\n',
  };
  for (const [path, content] of Object.entries({ ...defaults, ...pages })) write(root, path, content);
  return root;
}

function cli(root, ...args) {
  return spawnSync(process.execPath, [script, ...args], { env: { ...process.env, FLAT_DOCS_ROOT: root }, encoding: 'utf8' });
}

test('a tag becomes labelled fences in site order', () => {
  const out = generateFlatDocs(fixture()).get('intro.md');
  const labels = [...out.matchAll(/^\*\*(\w+)\*\*$/gm)].map((m) => m[1]);
  assert.deepEqual(labels, ['cURL', 'Python', 'Java', 'CLI']);
  assert.match(out, /\*\*cURL\*\*\n\n```bash\ncurl -f .*\n```\n\n\*\*Python\*\*\n\n```python\n/);
  assert.match(out, /```java\nclient\.a\(\);\n```/);
});

test('only existing variants are written', () => {
  const root = fixture({ 'docs/a.md': '<Example name="cases/only-curl" />\n' });
  const out = generateFlatDocs(root).get('a.md');
  assert.deepEqual([...out.matchAll(/^\*\*(\w+)\*\*$/gm)].map((m) => m[1]), ['cURL']);
  assert.doesNotMatch(out, /python/);
});

test('snippet equals extractSnippet output', () => {
  const out = generateFlatDocs(fixture()).get('intro.md');
  assert.ok(out.includes(`\`\`\`python\n${extractSnippet(PYTHON, 'python.py')}\n\`\`\``));
  assert.ok(out.includes(`\`\`\`bash\n${extractSnippet(CURL, 'curl.sh')}\n\`\`\``));
});

test('imports and tags are removed and front matter is kept', () => {
  const out = generateFlatDocs(fixture()).get('intro.md');
  assert.ok(out.startsWith('---\ntitle: Intro\ndescription: D\n---\n<!-- Generated from docs/intro.md. Do not edit by hand. -->\n\n# Intro'));
  assert.doesNotMatch(out, /^import /m);
  assert.doesNotMatch(out, /<Example/);
});

test('page without front matter starts with the notice', () => {
  const out = generateFlatDocs(fixture({ 'docs/b.md': '# B\n' })).get('b.md');
  assert.equal(out, '<!-- Generated from docs/b.md. Do not edit by hand. -->\n\n# B\n');
});

test('docs/openapi is skipped', () => {
  assert.deepEqual([...generateFlatDocs(fixture()).keys()], ['intro.md']);
});

test('a missing example fails with page and name', () => {
  const root = fixture({ 'docs/sub/c.md': '<Example name="cases/missing" />\n' });
  assert.throws(() => generateFlatDocs(root), /sub\/c\.md.*cases\/missing/);
  const result = cli(root);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /sub\/c\.md.*cases\/missing/);
});

test('site-absolute links are rewritten and relative ones are not', () => {
  const out = generateFlatDocs(fixture({ 'docs/d.md': '[f](/files/docudevs.yaml) [r](../x/y.md#a) <a href="/files/s.md" target="_blank">skill</a>\n' })).get('d.md');
  assert.ok(out.includes('[f](https://docs.docudevs.ai/files/docudevs.yaml)'));
  assert.ok(out.includes('[r](../x/y.md#a)'));
  assert.ok(out.includes('[skill](https://docs.docudevs.ai/files/s.md)'));
  assert.ok(generateFlatDocs(fixture()).get('intro.md').includes('[sibling](./guide.md)'));
});

test('tabs are unwrapped to labelled sections and fenced text is untouched', () => {
  const page = "import Tabs from '@theme/Tabs';\n\n<Tabs\n  defaultValue=\"a\"\n  values={[\n    {label: 'Alpha', value: 'a'},\n    {label: 'Beta', value: 'b'},\n  ]}>\n  <TabItem value=\"a\">\n\n```bash\nimport x from 'y'\n<Example name=\"keep\" />\n```\n\n  </TabItem>\n  <TabItem value=\"b\">\ntext\n  </TabItem>\n</Tabs>\n";
  const out = generateFlatDocs(fixture({ 'docs/t.md': page })).get('t.md');
  assert.ok(out.includes('**Alpha**\n\n```bash\nimport x from \'y\'\n<Example name="keep" />\n```'));
  assert.ok(out.includes('**Beta**\n\ntext'));
  assert.doesNotMatch(out, /<\/?Tabs|<\/?TabItem/);
});

test('output is deterministic with LF endings and one trailing newline', () => {
  const root = fixture({ 'docs/e.md': 'a\r\nb\r\n\r\n\r\n' });
  const first = generateFlatDocs(root);
  const second = generateFlatDocs(root);
  assert.deepEqual([...first], [...second]);
  const out = first.get('e.md');
  assert.doesNotMatch(out, /\r/);
  assert.ok(out.endsWith('b\n') && !out.endsWith('\n\n'));
});

test('write removes stale files and check reports problems', () => {
  const root = fixture();
  assert.equal(cli(root, '--check').status, 1);
  assert.match(cli(root, '--check').stderr, /missing: flat-docs\/intro\.md/);
  assert.equal(cli(root).status, 0);
  assert.equal(cli(root, '--check').status, 0);

  write(root, 'flat-docs/old/page.md', 'stale\n');
  const stale = cli(root, '--check');
  assert.equal(stale.status, 1);
  assert.match(stale.stderr, /stale: flat-docs\/old\/page\.md/);

  writeFileSync(join(root, 'flat-docs/intro.md'), 'changed\n');
  const changed = cli(root, '--check');
  assert.match(changed.stderr, /differs: flat-docs\/intro\.md/);
  assert.equal(readFileSync(join(root, 'flat-docs/intro.md'), 'utf8'), 'changed\n');

  assert.equal(cli(root).status, 0);
  assert.ok(!existsSync(join(root, 'flat-docs/old')));
  assert.equal(cli(root, '--check').status, 0);
  rmSync(join(root, 'flat-docs/intro.md'));
  assert.equal(runFlatDocs(root, { check: true }).code, 1);
});

test('context7.json satisfies the published schema constraints', () => {
  const config = JSON.parse(readFileSync(join(repoRoot, 'context7.json'), 'utf8'));
  const allowed = ['$schema', 'projectTitle', 'description', 'branch', 'folders', 'excludeFolders', 'excludeFiles', 'includeExtensions', 'rules', 'disallow', 'redirect', 'previousVersions', 'url', 'public_key'];
  for (const key of Object.keys(config)) assert.ok(allowed.includes(key), `unknown key ${key}`);
  assert.equal(config.$schema, 'https://context7.com/schema/context7.json');
  assert.equal(typeof config.url, 'string');
  assert.ok(config.url.length > 0);
  assert.equal(typeof config.public_key, 'string');
  assert.ok(config.public_key.length > 0);
  assert.ok(config.projectTitle.length >= 1);
  assert.ok(config.description.length >= 10 && config.description.length <= 500);
  for (const key of ['folders', 'excludeFolders']) {
    assert.ok(Array.isArray(config[key]) && config[key].length <= 50);
    for (const entry of config[key]) assert.ok(entry.length >= 1 && entry.length <= 255);
  }
  for (const folder of config.folders) assert.ok(existsSync(join(repoRoot, folder)), `missing folder ${folder}`);
  for (const folder of ['docs', 'examples', 'tools', 'src', 'static', 'build', 'node_modules']) {
    assert.ok(config.excludeFolders.includes(folder), `${folder} not excluded`);
  }
  assert.ok(config.rules.length >= 1 && config.rules.length <= 10);
  for (const rule of config.rules) assert.ok(rule.length >= 1 && rule.length <= 255);
});
