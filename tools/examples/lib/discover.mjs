import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { extractSnippet } from './snippet.mjs';

/** @typedef {'curl'|'python'|'java'|'cli'} Lang */
/** @typedef {{ lang: Lang, file: string, source: string, snippet: string }} ExampleFile */
/** @typedef {{ name: string, files: ExampleFile[] }} Example */
/** @typedef {{ value: Lang, label: string, language: string, code: string }} Tab */
/** @typedef {Record<string, { tabs: Tab[] }>} Manifest */

export const LANGS = [
  { value: 'curl', label: 'cURL', file: 'curl.sh', language: 'bash' },
  { value: 'python', label: 'Python', file: 'python.py', language: 'python' },
  { value: 'java', label: 'Java', file: 'Example.java', language: 'java' },
  { value: 'cli', label: 'CLI', file: 'cli.sh', language: 'bash' },
];

const visibleEntries = (dir) =>
  readdirSync(dir, { withFileTypes: true }).filter((entry) => !entry.name.startsWith('.'));

const sortedDirectories = (dir) =>
  visibleEntries(dir)
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort();

function readExample(examplesDir, name) {
  const dir = join(examplesDir, name);
  const known = new Set(LANGS.map((lang) => lang.file));
  const stray = visibleEntries(dir).filter((entry) => !known.has(entry.name));
  if (stray.length > 0) {
    throw new Error(`${name}: unrecognised file ${stray.map((entry) => entry.name).join(', ')}`);
  }

  const files = LANGS.filter((lang) => visibleEntries(dir).some((entry) => entry.name === lang.file)).map(
    (lang) => {
      const path = join(dir, lang.file);
      if (!statSync(path).isFile()) throw new Error(`${name}: ${lang.file} is not a file`);
      const source = readFileSync(path, 'utf8');
      return { lang: lang.value, file: `${name}/${lang.file}`, source, snippet: extractSnippet(source, `${name}/${lang.file}`) };
    },
  );
  if (files.length === 0) throw new Error(`${name}: no recognised example file`);
  return { name, files };
}

/** @returns {Example[]} */
export function discoverExamples(examplesDir) {
  return sortedDirectories(examplesDir).flatMap((area) =>
    sortedDirectories(join(examplesDir, area)).map((example) => readExample(examplesDir, `${area}/${example}`)),
  );
}
