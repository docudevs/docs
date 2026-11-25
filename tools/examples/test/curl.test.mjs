import test from 'node:test';
import assert from 'node:assert/strict';
import { extractCurlCommands, extractCurlRequests, findOperation, hostOf, urlIsAllowed } from '../lib/curl.mjs';

test('method defaults to GET, POST when -d or -F present, -X wins', () => {
  assert.deepEqual(extractCurlRequests('curl https://api.docudevs.ai/cases'), [{ method: 'GET', path: '/cases' }]);
  assert.deepEqual(
    extractCurlRequests('curl https://api.docudevs.ai/document/upload-files \\\n  -F "document=@invoice.pdf"'),
    [{ method: 'POST', path: '/document/upload-files' }],
  );
  assert.deepEqual(extractCurlRequests('curl -X DELETE "$API_URL/cases/$CASE_ID"'), [
    { method: 'DELETE', path: '/cases/{}' },
  ]);
});
test('--request long form wins over the implied method', () => {
  assert.deepEqual(extractCurlRequests('curl --request PUT -d "{}" "$API_URL/cases/$CASE_ID"'), [
    { method: 'PUT', path: '/cases/{}' },
  ]);
});
test('variables in the path become wildcards and the query string is dropped', () => {
  assert.deepEqual(extractCurlRequests('curl "https://api.docudevs.ai/job/${JOB_GUID}/result?format=csv"'), [
    { method: 'GET', path: '/job/{}/result' },
  ]);
});
test('a URL beginning with ${API_URL} is recognised', () => {
  assert.deepEqual(extractCurlRequests('curl "${API_URL}/cases/"'), [{ method: 'GET', path: '/cases' }]);
});
test('several curl commands in one file are all extracted', () => {
  assert.equal(extractCurlRequests('curl "$API_URL/a"\n\ncurl -X POST "$API_URL/b" \\\n  -d "{}"').length, 2);
});
test('a host other than the API or $API_URL is ignored', () => {
  assert.deepEqual(extractCurlRequests('curl https://example.com/file.pdf -o file.pdf'), []);
});
test('wildcard segments match templated spec paths', () => {
  const spec = { paths: { '/cases/{caseId}': { delete: {} } } };
  assert.equal(findOperation(spec, { method: 'DELETE', path: '/cases/{}' }), true);
  assert.equal(findOperation(spec, { method: 'POST', path: '/cases/{}' }), false);
});
test('a spec parameter segment matches a literal request segment', () => {
  const spec = { paths: { '/cases/{caseId}/documents': { get: {} } } };
  assert.equal(findOperation(spec, { method: 'GET', path: '/cases/abc/documents' }), true);
  assert.equal(findOperation(spec, { method: 'GET', path: '/cases/abc' }), false);
});
test('curl inside a command substitution with an assignment is extracted', () => {
  assert.deepEqual(extractCurlRequests('JOB=$(curl -s -X POST "$API_URL/x" | jq -r .guid)'), [
    { method: 'POST', path: '/x' },
  ]);
});
test('commands report the line of the curl word and the URL', () => {
  assert.deepEqual(extractCurlCommands('#!/usr/bin/env bash\n\ncurl "$API_URL/a"\ncurl \\\n  -X POST "https://api.docudevs.ai/b"'), [
    { line: 3, url: '$API_URL/a', request: { method: 'GET', path: '/a' } },
    { line: 4, url: 'https://api.docudevs.ai/b', request: { method: 'POST', path: '/b' } },
  ]);
});
test('a mistyped host yields a command with no request', () => {
  assert.deepEqual(extractCurlCommands('curl "https://api.docudevs.com/cases"'), [
    { line: 1, url: 'https://api.docudevs.com/cases', request: null },
  ]);
  assert.deepEqual(extractCurlRequests('curl "https://api.docudevs.com/cases"'), []);
});
test('a curl with no recognisable URL yields a command with a null url', () => {
  assert.deepEqual(extractCurlCommands('curl -s -S'), [{ line: 1, url: null, request: null }]);
});
test('a URL that is entirely another variable is reported as a non-API url', () => {
  assert.deepEqual(extractCurlCommands('curl -s -o out.pdf "$DOWNLOAD_URL"'), [
    { line: 1, url: '$DOWNLOAD_URL', request: null },
  ]);
  assert.deepEqual(extractCurlCommands('curl -d "$BODY" "$API_URL/x"')[0].request, { method: 'POST', path: '/x' });
});
test('hostOf returns the host, or null for a pure variable reference', () => {
  assert.equal(hostOf('https://example.com/file.pdf'), 'example.com');
  assert.equal(hostOf('${DOWNLOAD_URL}'), null);
});
test('urlIsAllowed accepts a variable reference or an allowlisted host', () => {
  assert.equal(urlIsAllowed('"$DOWNLOAD_URL"'.slice(1, -1), []), true);
  assert.equal(urlIsAllowed('https://example.com/x', ['example.com']), true);
  assert.equal(urlIsAllowed('https://api.docudevs.com/cases', ['example.com']), false);
  assert.equal(urlIsAllowed(null, []), false);
});
