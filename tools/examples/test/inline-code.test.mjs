import test from 'node:test';
import assert from 'node:assert/strict';
import { compareRatchet, findInlineCalls, findLanguageTabs } from '../lib/inline-code.mjs';

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
