import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';
import { discoverExamples } from './discover.mjs';
import { buildManifest } from './manifest.mjs';

export const SITE_URL = 'https://docs.docudevs.ai';
export const OUTPUT_DIR = 'flat-docs';

const DOC_FILE = /\.mdx?$/;
const FENCE = /^\s*(`{3,}|~{3,})/;
const EXAMPLE_TAG = /<Example\s+name\s*=\s*(?:"([^"]*)"|'([^']*)')\s*\/?>/g;
const TABS_OPEN = /^\s*<Tabs\b/;
const TABS_CLOSE = /^\s*<\/Tabs>\s*$/;
const TAB_ITEM_OPEN = /^\s*<TabItem\b([^>]*)>\s*$/;
const TAB_ITEM_CLOSE = /^\s*<\/TabItem>\s*$/;
const TAB_VALUE = /\{\s*label:\s*(?:'([^']*)'|"([^"]*)")\s*,\s*value:\s*(?:'([^']*)'|"([^"]*)")\s*\}/g;
const IMPORT_LINE = /^import\s.+\sfrom\s+['"][^'"]+['"];?\s*$/;
const ANCHOR = /<a\s+href="([^"]*)"[^>]*>(.*?)<\/a>/g;
const LOCAL_IMAGE = /!\[([^\]]*)\]\((?!https?:)[^)]*\)/g;
const SITE_ABSOLUTE_LINK = /\]\(\//g;
const SITE_ABSOLUTE_REFERENCE = /^(\s*\[[^\]]+\]:\s*)\//;

function attribute(attributes, name) {
  const match = new RegExp(`\\b${name}=(?:"([^"]*)"|'([^']*)')`).exec(attributes);
  return match ? (match[1] ?? match[2]) : null;
}

function fenceFor(code) {
  const longest = Math.max(0, ...[...code.matchAll(/`+/g)].map((match) => match[0].length));
  return '`'.repeat(Math.max(3, longest + 1));
}

function renderExample(example) {
  return example.tabs
    .map((tab) => {
      const fence = fenceFor(tab.code);
      return `**${tab.label}**\n\n${fence}${tab.language}\n${tab.code}\n${fence}`;
    })
    .join('\n\n');
}

function splitFrontMatter(text) {
  const match = /^---\n[\s\S]*?\n---\n/.exec(text);
  return match ? [match[0], text.slice(match[0].length)] : ['', text];
}

function absolutize(line) {
  return line
    .replace(ANCHOR, (_, href, label) => `[${label}](${href})`)
    .replace(LOCAL_IMAGE, (_, alt) => alt)
    .replace(SITE_ABSOLUTE_LINK, `](${SITE_URL}/`)
    .replace(SITE_ABSOLUTE_REFERENCE, `$1${SITE_URL}/`);
}

function collapseBlankLines(lines) {
  const result = [];
  let fence = null;
  for (const line of lines) {
    const match = FENCE.exec(line);
    if (fence) {
      if (match && match[1][0] === fence[0] && match[1].length >= fence.length) fence = null;
    } else if (match) {
      fence = match[1];
    } else if (line.trim() === '' && result.length > 0 && result[result.length - 1].trim() === '') {
      continue;
    }
    result.push(line);
  }
  return result;
}

function convertBody(body, page, manifest) {
  const output = [];
  let fence = null;
  let tabLabels = [];
  let pendingTabs = null;
  for (const line of body.split('\n')) {
    const fenceMatch = FENCE.exec(line);
    if (fence) {
      output.push(line);
      if (fenceMatch && fenceMatch[1][0] === fence[0] && fenceMatch[1].length >= fence.length) fence = null;
      continue;
    }
    if (fenceMatch) {
      fence = fenceMatch[1];
      output.push(line);
      continue;
    }
    if (pendingTabs !== null) {
      pendingTabs += `\n${line}`;
      if (line.includes('>')) {
        tabLabels = [...pendingTabs.matchAll(TAB_VALUE)].map((m) => [m[3] ?? m[4], m[1] ?? m[2]]);
        pendingTabs = null;
      }
      continue;
    }
    if (TABS_OPEN.test(line)) {
      if (line.trimEnd().endsWith('>')) tabLabels = [...line.matchAll(TAB_VALUE)].map((m) => [m[3] ?? m[4], m[1] ?? m[2]]);
      else pendingTabs = line;
      continue;
    }
    if (TABS_CLOSE.test(line) || TAB_ITEM_CLOSE.test(line) || IMPORT_LINE.test(line)) continue;
    const tabItem = TAB_ITEM_OPEN.exec(line);
    if (tabItem) {
      const value = attribute(tabItem[1], 'value');
      const label = attribute(tabItem[1], 'label') ?? tabLabels.find(([v]) => v === value)?.[1] ?? value;
      output.push('', `**${label}**`, '');
      continue;
    }
    if (EXAMPLE_TAG.test(line)) {
      EXAMPLE_TAG.lastIndex = 0;
      const replaced = line.replace(EXAMPLE_TAG, (_, double, single) => {
        const name = double ?? single;
        if (!Object.hasOwn(manifest, name)) throw new Error(`${page}: unknown example "${name}"`);
        return `\n${renderExample(manifest[name])}\n`;
      });
      output.push(...replaced.split('\n'));
      continue;
    }
    EXAMPLE_TAG.lastIndex = 0;
    output.push(absolutize(line));
  }
  return collapseBlankLines(output).join('\n');
}

export function flattenPage(text, page, manifest) {
  const [frontMatter, body] = splitFrontMatter(text.replace(/\r\n/g, '\n'));
  const notice = `<!-- Generated from docs/${page}. Do not edit by hand. -->`;
  const converted = convertBody(body, page, manifest).replace(/^\n+/, '');
  return `${frontMatter}${notice}\n\n${converted.replace(/\n+$/, '')}\n`;
}

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

function listFiles(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => relative(dir, join(entry.parentPath, entry.name)).split(sep).join('/'))
    .sort();
}

export function generateFlatDocs(root) {
  const docsDir = join(root, 'docs');
  const manifest = buildManifest(discoverExamples(join(root, 'examples')));
  const files = new Map();
  for (const path of walk(docsDir, docsDir)) {
    const page = relative(docsDir, path).split(sep).join('/');
    const target = page.replace(/\.mdx$/, '.md');
    files.set(target, flattenPage(readFileSync(path, 'utf8'), page, manifest));
  }
  return files;
}

export function compareFlatDocs(root, generated) {
  const outputDir = join(root, OUTPUT_DIR);
  const existing = new Set(listFiles(outputDir));
  const problems = [];
  for (const [file, content] of generated) {
    if (!existing.has(file)) problems.push(`missing: ${OUTPUT_DIR}/${file}`);
    else if (readFileSync(join(outputDir, file), 'utf8') !== content) problems.push(`differs: ${OUTPUT_DIR}/${file}`);
  }
  for (const file of existing) {
    if (!generated.has(file)) problems.push(`stale: ${OUTPUT_DIR}/${file}`);
  }
  return problems.sort();
}

function removeEmptyDirectories(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) removeEmptyDirectories(join(dir, entry.name));
  }
  if (readdirSync(dir).length === 0) rmSync(dir, { recursive: true });
}

export function writeFlatDocs(root, generated) {
  const outputDir = join(root, OUTPUT_DIR);
  for (const file of listFiles(outputDir)) {
    if (!generated.has(file)) rmSync(join(outputDir, file));
  }
  for (const [file, content] of generated) {
    const path = join(outputDir, file);
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, content);
  }
  if (existsSync(outputDir)) removeEmptyDirectories(outputDir);
  mkdirSync(outputDir, { recursive: true });
}

export function runFlatDocs(root, { check = false } = {}) {
  let generated;
  try {
    generated = generateFlatDocs(root);
  } catch (error) {
    return { code: 1, message: error.message };
  }
  if (check) {
    const problems = compareFlatDocs(root, generated);
    if (problems.length > 0) {
      return { code: 1, message: `${OUTPUT_DIR} is out of date, run npm run docs:flat\n${problems.join('\n')}` };
    }
    return { code: 0, message: `${OUTPUT_DIR} is up to date: ${generated.size} pages` };
  }
  writeFlatDocs(root, generated);
  return { code: 0, message: `Wrote ${generated.size} pages to ${OUTPUT_DIR}` };
}
