import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  REFERENCE_PAGES,
  compareRatchet,
  findInlineCalls,
  findLanguageTabs,
  readClientMethods,
} from './lib/inline-code.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const docsDir = process.argv[2] ?? join(root, 'docs');

function pagesUnder(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return pagesUnder(path);
    return /\.mdx?$/.test(entry.name) ? [path] : [];
  });
}

function parseRatchet(text) {
  return new Map(
    text
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line !== '' && !line.startsWith('#'))
      .map((line) => {
        const split = line.lastIndexOf(':');
        return [line.slice(0, split), Number(line.slice(split + 1))];
      }),
  );
}

function ratchetProblems(file, actual) {
  const known = parseRatchet(readFileSync(join(root, 'examples', file), 'utf8'));
  return compareRatchet(actual, known).map((problem) => `examples/${file}: ${problem}`);
}

try {
  const methods = readClientMethods();
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
  const problems = [
    ...ratchetProblems('inline-language-tabs.txt', tabs),
    ...ratchetProblems('inline-calls.txt', calls),
  ];
  if (problems.length > 0) {
    console.error(problems.join('\n'));
    process.exit(1);
  }
  console.log(`Inline code OK: ${tabs.size} pages with language tabs, ${calls.size} pages with inline calls`);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
