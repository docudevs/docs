import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  compareRatchet,
  formatRatchet,
  parseRatchet,
  ratchetOrderProblems,
  scanDocs,
} from './lib/inline-code.mjs';

const root = process.env.INLINE_ROOT ?? join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const args = process.argv.slice(2);
const write = args.includes('--write');
const docsDir = args.find((arg) => !arg.startsWith('--')) ?? join(root, 'docs');

const RATCHETS = [
  {
    file: 'inline-language-tabs.txt',
    key: 'tabs',
    header: '# Pages that hold inline language tabs, with the tab count. This file has no entries and stays that way: put the code in an example.',
  },
  {
    file: 'inline-calls.txt',
    key: 'calls',
    header: '# Pages that hold inline fences calling the product, with the fence count. This file has no entries and stays that way: put the code in an example.',
  },
];

try {
  const scanned = scanDocs(docsDir);
  if (write) {
    for (const { file, key, header } of RATCHETS) {
      writeFileSync(join(root, 'examples', file), formatRatchet(header, scanned[key]));
    }
  }
  const problems = RATCHETS.flatMap(({ file, key }) => {
    const text = readFileSync(join(root, 'examples', file), 'utf8');
    return [...ratchetOrderProblems(text), ...compareRatchet(scanned[key], parseRatchet(text))].map(
      (problem) => `examples/${file}: ${problem}`,
    );
  });
  if (problems.length > 0) {
    console.error(problems.join('\n'));
    process.exit(1);
  }
  console.log(
    `Inline code OK: ${scanned.tabs.size} pages with language tabs, ${scanned.calls.size} pages with inline calls`,
  );
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
