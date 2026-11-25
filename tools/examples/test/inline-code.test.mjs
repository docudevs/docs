import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  compareRatchet,
  findInlineCalls,
  findLanguageTabs,
  formatRatchet,
  parseRatchet,
  ratchetOrderProblems,
  scanDocs,
} from '../lib/inline-code.mjs';

const fence = (lang, body) => `\`\`\`${lang}\n${body}\n\`\`\`\n`;
const methods = ['create_case', 'list_cases'];
const calls = (markdown) => findInlineCalls(markdown, methods);

test('findLanguageTabs counts product language tabs', () => {
  assert.equal(findLanguageTabs('<TabItem value="python" label="Python">\n<TabItem value="curl" label="cURL">'), 2);
});
test('findLanguageTabs ignores tabs of other purposes', () => {
  assert.equal(findLanguageTabs('<TabItem value="pip">\n<TabItem value="uv">\n<TabItem value="invoice">'), 0);
});
test('findLanguageTabs accepts java, cli and sdk case-insensitively', () => {
  assert.equal(findLanguageTabs('<TabItem value="Java">\n<TabItem value="CLI">\n<TabItem value="sdk">'), 3);
});

test('python fence with an awaited client call counts', () => {
  assert.equal(calls(fence('python', 'await client.create_case(name="x")')), 1);
});
test('python fence with a call on another receiver name counts', () => {
  assert.equal(calls(fence('python', 'await docudevs.create_case(name="x")')), 1);
});
test('python fence with DocuDevsClient construction counts', () => {
  assert.equal(calls(fence('python', 'client = DocuDevsClient(token=key)')), 1);
});
test('python fence building a dict is not a call', () => {
  assert.equal(calls(fence('python', 'payload = {"name": "x"}\nprint(payload.get("name"))')), 0);
});
test('a python fence counts once however many calls it holds', () => {
  assert.equal(calls(fence('python', 'await client.create_case(name="x")\nawait client.list_cases()')), 1);
});
test('curl with a shell variable URL counts', () => {
  assert.equal(calls(fence('bash', 'curl "$BASE_URL/cases"')), 1);
  assert.equal(calls(fence('bash', 'curl "${API_URL}/cases"')), 1);
});
test('curl to the API host counts', () => {
  assert.equal(calls(fence('bash', 'curl -X POST "https://api.docudevs.ai/cases"')), 1);
});
test('curl to another host is not a call', () => {
  assert.equal(calls(fence('bash', 'curl https://example.com')), 0);
});
test('untagged and sh fences are inspected', () => {
  assert.equal(calls(fence('', 'curl "$BASE_URL/cases"')), 1);
  assert.equal(calls(fence('sh', 'docudevs cases list')), 1);
});
test('docudevs command counts', () => {
  assert.equal(calls(fence('bash', 'docudevs cases list')), 1);
});
test('docudevs version and help are not calls', () => {
  assert.equal(calls(fence('bash', 'docudevs --version')), 0);
  assert.equal(calls(fence('bash', 'docudevs --help')), 0);
  assert.equal(calls(fence('bash', 'docudevs cases --help')), 0);
});
test('java fence with DocuDevsClient counts', () => {
  assert.equal(calls(fence('java', 'var client = DocuDevsClient.builder().build();')), 1);
});
test('installs, json and output are not calls', () => {
  assert.equal(calls(fence('bash', 'pip install docu-devs-api-client')), 0);
  assert.equal(calls(fence('json', '{"a": 1}')), 0);
  assert.equal(calls(fence('java', 'int a = 1;')), 0);
});
test('fence counting adds up across languages', () => {
  const page = fence('python', 'client.list_cases()') + fence('bash', 'docudevs cases list') + fence('java', 'DocuDevsClient.builder()');
  assert.equal(calls(page), 3);
});

test('compareRatchet accepts equal counts', () => {
  assert.deepEqual(compareRatchet(new Map([['a', 3]]), new Map([['a', 3]])), []);
});
test('compareRatchet reports a count above the listed one', () => {
  const problems = compareRatchet(new Map([['a', 4]]), new Map([['a', 3]]));
  assert.equal(problems.length, 1);
  assert.match(problems[0], /a/);
});
test('compareRatchet tells the author to lower a line', () => {
  const problems = compareRatchet(new Map([['a', 2]]), new Map([['a', 3]]));
  assert.equal(problems.length, 1);
  assert.match(problems[0], /lower/);
});
test('compareRatchet reports an unlisted page with a count', () => {
  const problems = compareRatchet(new Map([['b', 1]]), new Map());
  assert.equal(problems.length, 1);
  assert.match(problems[0], /b/);
});
test('compareRatchet tells the author to remove a line that is now zero', () => {
  const problems = compareRatchet(new Map(), new Map([['a', 1]]));
  assert.equal(problems.length, 1);
  assert.match(problems[0], /remove/);
});

test('CRLF pages are handled', () => {
  assert.equal(calls('```bash\r\ndocudevs cases list\r\n```\r\n'), 1);
});
test('python: any method on a client receiver counts', () => {
  assert.equal(calls(fence('python', 'await client.update_webhook_settings(x)')), 1);
});
test('python: chained call across lines counts', () => {
  assert.equal(calls(fence('python', 'x = (client\n    .create_case(name="a"))')), 1);
});
test('python: AuthenticatedClient and docudevs imports count', () => {
  assert.equal(calls(fence('python', 'c = AuthenticatedClient(base_url="https://api.docudevs.ai")')), 1);
  assert.equal(calls(fence('python', 'from docudevs.api.llm_providers import create_llm_provider')), 1);
  assert.equal(calls(fence('python', 'import docudevs')), 1);
});
test('python: generic method names on other receivers do not count', () => {
  assert.equal(calls(fence('python', 'm = re.search(p, s)')), 0);
  assert.equal(calls(fence('python', 'future.result()')), 0);
  assert.equal(calls(fence('python', 'self.search(q)')), 0);
});
test('python: underscore method names count on any receiver', () => {
  assert.equal(calls(findInlineCallsFixture()), 1);
});
function findInlineCallsFixture() {
  return fence('python', 'await docudevs.create_case(name="x")');
}
test('python: method names without underscore need the client receiver', () => {
  assert.equal(findInlineCalls(fence('python', 'self.search(q)'), ['search']), 0);
  assert.equal(findInlineCalls(fence('python', 'client.search(q)'), ['search']), 1);
});
test('shell: docudevs must be a whole word', () => {
  assert.equal(calls(fence('bash', 'docudevs-cli run')), 0);
});
test('shell: docudevs after pipes and separators counts', () => {
  assert.equal(calls(fence('bash', 'cat x | docudevs cases list')), 1);
  assert.equal(calls(fence('bash', 'cd x && docudevs cases list')), 1);
  assert.equal(calls(fence('bash', 'a; docudevs cases list')), 1);
  assert.equal(calls(fence('bash', 'a || docudevs cases list')), 1);
  assert.equal(calls(fence('bash', '$ docudevs cases list')), 1);
});
test('shell: docudevs help after a separator is still not a call', () => {
  assert.equal(calls(fence('bash', 'cd x && docudevs --version')), 0);
});
test('shell: curl URL is judged per command', () => {
  assert.equal(calls(fence('bash', 'curl https://example.com/x -o "$DIR/out"')), 0);
  assert.equal(calls(fence('bash', 'curl -s "$BASE_URL/cases"')), 1);
  assert.equal(calls(fence('bash', 'curl "${API_URL:-https://api.docudevs.ai}/cases"')), 1);
  assert.equal(calls(fence('bash', 'curl "https://<generated-domain>/api/documents"')), 1);
});
test('shell: continued curl lines are one command', () => {
  assert.equal(calls(fence('bash', 'curl -X POST \\\n  -H "Authorization: $API_KEY" \\\n  "https://api.docudevs.ai/cases"')), 1);
  assert.equal(calls(fence('bash', 'curl -X POST \\\n  -H "Authorization: $API_KEY" \\\n  https://example.com/x')), 0);
});
test('shell: a second curl in the fence is judged on its own', () => {
  assert.equal(calls(fence('bash', 'curl https://example.com/a\ncurl "$BASE_URL/cases"')), 1);
});
test('fence parsing: tilde fences, indentation and attributes', () => {
  assert.equal(calls('~~~bash\ndocudevs cases list\n~~~\n'), 1);
  assert.equal(calls('<TabItem value="x">\n  ```bash\n  docudevs cases list\n  ```\n</TabItem>'), 1);
  assert.equal(calls('```python title="x"\nawait client.list_cases()\n```\n'), 1);
});
test('fence parsing: an inner fence belongs to the outer one', () => {
  const sample = '````markdown\n```bash\ndocudevs cases list\n```\n````\n';
  assert.equal(calls(sample), 0);
  assert.equal(calls('````bash\n```\ndocudevs cases list\n```\n````\n'), 1);
});

const scratch = () => mkdtempSync(join(tmpdir(), 'inline-'));
function project(files) {
  const root = scratch();
  for (const [path, content] of Object.entries(files)) {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    writeFileSync(join(root, path), content);
  }
  writeFileSync(join(root, 'tools-methods.json'), JSON.stringify(methods));
  return root;
}
const cli = fileURLToPath(new URL('../check-inline-code.mjs', import.meta.url));
const runCli = (root, ...args) =>
  spawnSync('node', [cli, ...args], { cwd: root, encoding: 'utf8', env: { ...process.env, INLINE_ROOT: root } });

const pageWith = '```bash\ndocudevs cases list\n```\n';

test('scanDocs skips openapi pages and exempts reference pages from calls', () => {
  const root = project({
    'docs/a.md': pageWith,
    'docs/openapi/x.md': pageWith + '<TabItem value="python">',
    'docs/reference/sdk-methods.md': pageWith + '<TabItem value="python">',
  });
  const { tabs, calls: found } = scanDocs(join(root, 'docs'), methods);
  assert.deepEqual([...found], [['docs/a.md', 1]]);
  assert.deepEqual([...tabs], [['docs/reference/sdk-methods.md', 1]]);
});
test('parseRatchet ignores comments and blank lines', () => {
  assert.deepEqual([...parseRatchet('# c\n\ndocs/a.md:2\n')], [['docs/a.md', 2]]);
});
test('ratchetOrderProblems flags unsorted page lines', () => {
  assert.equal(ratchetOrderProblems('docs/a.md:1\ndocs/b.md:1\n').length, 0);
  assert.equal(ratchetOrderProblems('docs/b.md:1\ndocs/a.md:1\n').length, 1);
});
test('formatRatchet writes sorted lines under a header', () => {
  const text = formatRatchet('# h', new Map([['docs/b.md', 1], ['docs/a.md', 2]]));
  assert.equal(text, '# h\ndocs/a.md:2\ndocs/b.md:1\n');
});
test('the CLI fails on an unsorted ratchet file and --write fixes it', () => {
  const root = project({
    'docs/a.md': pageWith,
    'docs/b.md': pageWith,
    'examples/inline-language-tabs.txt': '',
    'examples/inline-calls.txt': 'docs/b.md:1\ndocs/a.md:1\n',
  });
  assert.equal(runCli(root).status, 1);
  assert.equal(runCli(root, '--write').status, 0);
  assert.equal(readFileSync(join(root, 'examples/inline-calls.txt'), 'utf8').endsWith('docs/a.md:1\ndocs/b.md:1\n'), true);
  assert.equal(runCli(root).status, 0);
});
