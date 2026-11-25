import test from 'node:test';
import assert from 'node:assert/strict';
import { extractSnippet } from '../lib/snippet.mjs';

test('whole file without markers, shebang dropped', () => {
  assert.equal(extractSnippet('#!/bin/bash\ncurl x\n', 'curl.sh'), 'curl x');
});
test('region between markers is dedented', () => {
  const src = 'class A {\n  void m() {\n    // docs:start\n    client.listCases();\n    // docs:end\n  }\n}\n';
  assert.equal(extractSnippet(src, 'Example.java'), 'client.listCases();');
});
test('two regions joined by a blank line', () => {
  const src = '# docs:start\na = 1\n# docs:end\nx = 0\n# docs:start\nb = 2\n# docs:end\n';
  assert.equal(extractSnippet(src, 'python.py'), 'a = 1\n\nb = 2');
});
test('start without end fails naming the file', () => {
  assert.throws(() => extractSnippet('# docs:start\na = 1\n', 'cases/create/python.py'),
    /cases\/create\/python\.py.*docs:start.*docs:end/s);
});
test('end without start fails naming the file', () => {
  assert.throws(() => extractSnippet('a = 1\n# docs:end\n', 'cases/create/python.py'), /cases\/create\/python\.py/);
});
test('CRLF input yields LF output', () => {
  assert.equal(extractSnippet('a = 1\r\nb = 2\r\n', 'python.py'), 'a = 1\nb = 2');
});
test('an empty marked region adds no blank separator', () => {
  const src = '# docs:start\na = 1\n# docs:end\n# docs:start\n\n# docs:end\n# docs:start\nb = 2\n# docs:end\n';
  assert.equal(extractSnippet(src, 'python.py'), 'a = 1\n\nb = 2');
});
