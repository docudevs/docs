import { readdirSync, readFileSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const DOC_FILE = /\.mdx?$/;
const FENCE = /^\s*(`{3,}|~{3,})/;
const EXAMPLE_TAG = /<Example\s+name\s*=\s*(?:"([^"]*)"|'([^']*)')/g;

function* walk(dir, root) {
  for (const entry of readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      if (dir === root && entry.name === 'openapi') continue;
      yield* walk(path, root);
    } else if (DOC_FILE.test(entry.name)) {
      yield path;
    }
  }
}

function withoutFencedBlocks(text) {
  let fence = null;
  return text
    .split('\n')
    .map((line) => {
      const match = FENCE.exec(line);
      if (fence) {
        if (match && match[1][0] === fence[0] && match[1].length >= fence.length) fence = null;
        return '';
      }
      if (match) {
        fence = match[1];
        return '';
      }
      return line;
    })
    .join('\n');
}

/** @returns {{ page: string, name: string }[]} */
export function findReferences(docsDir) {
  const refs = [];
  for (const path of walk(docsDir, docsDir)) {
    const page = relative(docsDir, path).split(sep).join('/');
    const text = withoutFencedBlocks(readFileSync(path, 'utf8'));
    for (const match of text.matchAll(EXAMPLE_TAG)) {
      refs.push({ page, name: match[1] ?? match[2] });
    }
  }
  return refs.sort((a, b) => (a.page < b.page ? -1 : a.page > b.page ? 1 : 0));
}
