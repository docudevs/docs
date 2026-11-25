import { readdirSync, readFileSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const TAB_LANGUAGES = new Set(['python', 'curl', 'java', 'cli', 'sdk']);
const SHELL_LANGUAGES = new Set(['', 'bash', 'sh', 'shell']);
const TAB_ITEM = /<TabItem\b[^>]*\bvalue=["']([^"']+)["']/g;
const OPENING_FENCE = /^(\s*)(`{3,}|~{3,})\s*([^\s`]*)/;
const LINE_BREAK = /\r?\n/;
const PYTHON_PRODUCT = /\b(?:DocuDevsClient|AuthenticatedClient)\(|^\s*(?:from|import)\s+docudevs\b|\bclient\s*\.\s*\w+\(/m;
const COMMAND_START = String.raw`(?:^\s*|[|;&]\s*)(?:\$\s+)?`;
const DOCUDEVS_COMMAND = new RegExp(`${COMMAND_START}docudevs(?:\\s+(.*))?$`);
const CURL_COMMAND = new RegExp(`${COMMAND_START}curl(?:\\s+(.*))?$`);
const NON_CALL_ARGUMENTS = /^\s*(?:--version|--help|[\w-]+\s+--help)\s*$/;
const TOKEN = /"[^"]*"|'[^']*'|\S+/g;
const URL_START = /^["']?(?:https?:|\$|<)/;
const OPTIONS_WITH_VALUE = new Set(['-H', '--header', '-d', '--data', '--data-raw', '--data-binary', '--data-urlencode', '-F', '--form', '-X', '--request', '-o', '--output', '-u', '--user', '-w', '--write-out', '-A', '--user-agent', '-b', '--cookie', '-T', '--upload-file', '--url-query']);
const API_HOST = /^https?:\/\/api\.docudevs\.ai(?:[/:?]|$)/;
const PLACEHOLDER_HOST = /^https?:\/\/<[^/\s>]*>/;

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
  const lines = markdown.split(LINE_BREAK);
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

function logicalCommands(code) {
  return code.replace(/\\\n\s*/g, ' ').split('\n');
}

function firstUrl(command) {
  const tokens = command.match(TOKEN) ?? [];
  for (let index = 0; index < tokens.length; index += 1) {
    if (OPTIONS_WITH_VALUE.has(tokens[index])) index += 1;
    else if (URL_START.test(tokens[index])) return tokens[index].replace(/^["']/, '');
  }
  return null;
}

function isProductCurl(command) {
  const url = firstUrl(command);
  return url !== null && (url.startsWith('$') || API_HOST.test(url) || PLACEHOLDER_HOST.test(url));
}

function callsFromShell(code) {
  return logicalCommands(code).some((command) => {
    const docudevs = DOCUDEVS_COMMAND.exec(command);
    if (docudevs && !NON_CALL_ARGUMENTS.test(docudevs[1] ?? '')) return true;
    const curl = CURL_COMMAND.exec(command);
    return curl !== null && isProductCurl(curl[1] ?? '');
  });
}

function callsProduct(fence, underscoreCall) {
  if (fence.lang === 'python') return PYTHON_PRODUCT.test(fence.code) || underscoreCall.test(fence.code);
  if (fence.lang === 'java') return fence.code.includes('DocuDevsClient');
  return SHELL_LANGUAGES.has(fence.lang) && callsFromShell(fence.code);
}

export function findInlineCalls(markdown, methodNames = readClientMethods()) {
  const names = methodNames.filter((name) => name.includes('_'));
  const underscoreCall = names.length === 0 ? /(?!)/ : new RegExp(`\\b[A-Za-z_]\\w*\\.(?:${names.join('|')})\\(`);
  return extractFences(markdown).filter((fence) => callsProduct(fence, underscoreCall)).length;
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

const ratchetPages = (text) =>
  text
    .split(LINE_BREAK)
    .map((line) => line.trim())
    .filter((line) => line !== '' && !line.startsWith('#'));

export function parseRatchet(text) {
  return new Map(
    ratchetPages(text).map((line) => {
      const split = line.lastIndexOf(':');
      return [line.slice(0, split), Number(line.slice(split + 1))];
    }),
  );
}

export function ratchetOrderProblems(text) {
  const pages = ratchetPages(text).map((line) => line.slice(0, line.lastIndexOf(':')));
  return pages.flatMap((page, index) =>
    index > 0 && page < pages[index - 1] ? [`${page}: lines must be sorted by page path`] : [],
  );
}

export function formatRatchet(header, counts) {
  const lines = [...counts].sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)).map(([page, count]) => `${page}:${count}`);
  return `${header}\n${lines.join('\n')}\n`;
}

function pagesUnder(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return pagesUnder(path);
    return /\.mdx?$/.test(entry.name) ? [path] : [];
  });
}

export function scanDocs(docsDir, methods = readClientMethods()) {
  const tabs = new Map();
  const calls = new Map();
  for (const path of pagesUnder(docsDir).sort()) {
    const page = relative(join(docsDir, '..'), path).split(sep).join('/');
    if (page.startsWith('docs/openapi/')) continue;
    const markdown = readFileSync(path, 'utf8');
    const tabCount = findLanguageTabs(markdown);
    if (tabCount > 0) tabs.set(page, tabCount);
    const callCount = REFERENCE_PAGES.has(page) ? 0 : findInlineCalls(markdown, methods);
    if (callCount > 0) calls.set(page, callCount);
  }
  return { tabs, calls };
}
