import { readFileSync } from 'node:fs';

const TAB_LANGUAGES = new Set(['python', 'curl', 'java', 'cli', 'sdk']);
const SHELL_LANGUAGES = new Set(['', 'bash', 'sh', 'shell']);
const TAB_ITEM = /<TabItem\b[^>]*\bvalue=["']([^"']+)["']/g;
const OPENING_FENCE = /^(\s*)(`{3,}|~{3,})\s*([^\s`]*)/;
const API_URL = /https?:\/\/api\.docudevs\.ai\b|[\s"']\$\{?[A-Za-z_]\w*\}?\//;
const CURL = /\bcurl\b/;
const DOCUDEVS_LINE = /^\s*(?:\$\s+)?docudevs\b(.*)$/;
const NON_CALL_ARGUMENTS = /^\s*(?:--version|--help|[\w-]+\s+--help)\s*$/;

export const REFERENCE_PAGES = new Set([
  'docs/reference/sdk-methods.md',
  'docs/reference/cli-reference.md',
  'docs/core/playbook-evaluation.md',
]);

const defaultMethodsFile = new URL('../client-methods.json', import.meta.url);

export function readClientMethods(file = defaultMethodsFile) {
  return JSON.parse(readFileSync(file, 'utf8'));
}

export function findLanguageTabs(markdown) {
  return [...markdown.matchAll(TAB_ITEM)].filter((match) => TAB_LANGUAGES.has(match[1].toLowerCase())).length;
}

function extractFences(markdown) {
  const fences = [];
  const lines = markdown.split('\n');
  let index = 0;
  while (index < lines.length) {
    const opening = OPENING_FENCE.exec(lines[index]);
    index += 1;
    if (!opening) continue;
    const [, , marker, lang] = opening;
    const body = [];
    while (index < lines.length && !closesFence(lines[index], marker)) {
      body.push(lines[index]);
      index += 1;
    }
    index += 1;
    fences.push({ lang: lang.toLowerCase(), code: body.join('\n') });
  }
  return fences;
}

function closesFence(line, marker) {
  const trimmed = line.trim();
  return trimmed.startsWith(marker) && new Set(trimmed).size === 1;
}

function callsProduct(fence, methodCall) {
  if (fence.lang === 'python') return fence.code.includes('DocuDevsClient(') || methodCall.test(fence.code);
  if (fence.lang === 'java') return fence.code.includes('DocuDevsClient');
  if (!SHELL_LANGUAGES.has(fence.lang)) return false;
  if (CURL.test(fence.code) && API_URL.test(fence.code)) return true;
  return fence.code.split('\n').some((line) => {
    const match = DOCUDEVS_LINE.exec(line);
    return match !== null && !NON_CALL_ARGUMENTS.test(match[1]);
  });
}

export function findInlineCalls(markdown, methodNames = readClientMethods()) {
  const methodCall =
    methodNames.length === 0 ? /(?!)/ : new RegExp(`\\b[A-Za-z_]\\w*\\.(?:${methodNames.join('|')})\\(`);
  return extractFences(markdown).filter((fence) => callsProduct(fence, methodCall)).length;
}

export function compareRatchet(actual, known) {
  const problems = [];
  for (const [page, count] of actual) {
    const listed = known.get(page) ?? 0;
    if (count > listed) {
      problems.push(`${page}: ${count} inline, ${listed} listed; convert the new blocks to examples instead of raising the count`);
    }
  }
  for (const [page, listed] of known) {
    const count = actual.get(page) ?? 0;
    if (count >= listed) continue;
    problems.push(
      count === 0
        ? `${page}: no inline blocks left; remove the line from the ratchet file`
        : `${page}: ${count} inline, ${listed} listed; lower the line in the ratchet file to ${count}`,
    );
  }
  return problems;
}
