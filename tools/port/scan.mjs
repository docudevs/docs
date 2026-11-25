import { execFileSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { extname, join } from 'node:path';
import { pathToFileURL } from 'node:url';

const ALLOWED_HOSTS = ['api.docudevs.ai', 'docs.docudevs.ai', 'docudevs.ai', 'example.com', 'example.org', 'json-schema.org', 'jqlang.org', 'pypi.org', 'example.openai.azure.com', 'example.cognitiveservices.azure.com'];
const PLACEHOLDER_HOSTS = ['example.com', 'example.org'];
const PUBLIC_GITHUB_PATH = /^\/docudevs(?:[/?#]|$)/i;
const SCANNER_PATHS = ['tools/port/', 'tools/examples/test/port-scan.test.mjs'];
const ALLOWED_EMAIL_DOMAINS = ['docudevs.ai', 'example.com', 'example.org'];
const MARKDOWN = new Set(['.md', '.mdx']);
const SKIPPED_DIRECTORIES = new Set(['node_modules', '.git', '.superpowers']);

const ANYWHERE_TERMS = [
  '/internal/',
  'localhost',
  '127.0.0.1',
  'azurecontainerapps.io',
  'docudevs-doc',
  'DOCUDEVS_API_KEY',
  'Bearer $API_KEY',
];

const EMAIL = /[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+)/g;
const URL_HOST = /https?:\/\/([A-Za-z0-9.-]+)(\/[^\s"'`)>\]]*)?/g;
const HEX_TOKEN = /(?<![0-9A-Fa-f])[0-9A-Fa-f]{32,}(?![0-9A-Fa-f])/;
const BASE64_TOKEN = /[A-Za-z0-9+_-]{32,}={0,2}/g;

function looksLikeBase64Token(candidate) {
  return /[a-z]/.test(candidate) && /[A-Z]/.test(candidate) && /[0-9]/.test(candidate);
}

function hostIsAllowed(host, path) {
  if (host === 'github.com') return PUBLIC_GITHUB_PATH.test(path);
  if (PLACEHOLDER_HOSTS.some((suffix) => host.endsWith(`.${suffix}`))) return true;
  return ALLOWED_HOSTS.includes(host);
}

function codeFindings(line) {
  const found = [];
  for (const [, host, path = ''] of line.matchAll(URL_HOST)) {
    if (!hostIsAllowed(host.toLowerCase(), path)) found.push('non-API host');
  }
  if (HEX_TOKEN.test(line) || [...line.matchAll(BASE64_TOKEN)].some(([token]) => looksLikeBase64Token(token))) {
    found.push('token-like string');
  }
  return found;
}

function lineFindings(line, inCode, literals) {
  const found = [];
  literals.forEach((literal, index) => {
    if (literal !== '' && line.includes(literal)) found.push(`forbidden literal #${index + 1}`);
  });
  for (const term of ANYWHERE_TERMS) {
    if (line.includes(term)) found.push(`forbidden text ${term}`);
  }
  for (const [address, domain] of line.matchAll(EMAIL)) {
    if (!ALLOWED_EMAIL_DOMAINS.includes(domain.toLowerCase())) found.push('email address outside allowed domains');
  }
  if (inCode) found.push(...codeFindings(line));
  return found;
}

/** @returns {string[]} findings as "<file>:<line>: <message>" */
export function scanText(text, file, extraLiterals) {
  const markdown = MARKDOWN.has(extname(file).toLowerCase());
  const findings = [];
  let inFence = false;
  text.split('\n').forEach((line, index) => {
    if (markdown && /^\s*(```|~~~)/.test(line)) {
      inFence = !inFence;
      return;
    }
    const inCode = !markdown || inFence;
    for (const message of lineFindings(line, inCode, extraLiterals)) findings.push(`${file}:${index + 1}: ${message}`);
  });
  return findings;
}

function scanAddedLines(base, literals) {
  const diff = execFileSync('git', ['diff', '--unified=0', '--no-color', `${base}...HEAD`], {
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
  });
  const findings = [];
  let file = null;
  let inHeader = false;
  let line = 0;
  for (const row of diff.split('\n')) {
    if (row.startsWith('diff --git ')) {
      inHeader = true;
      file = null;
    } else if (inHeader) {
      if (row.startsWith('+++ ')) file = row === '+++ /dev/null' ? null : row.slice(6);
      else if (row.startsWith('@@')) inHeader = false;
    }
    if (inHeader) continue;
    if (row.startsWith('@@')) {
      line = Number(/\+(\d+)/.exec(row)[1]);
    } else if (file && row.startsWith('+') && !SCANNER_PATHS.some((path) => file.startsWith(path))) {
      for (const message of lineFindings(row.slice(1), true, literals)) findings.push(`${file}:${line}: ${message}`);
      line++;
    } else if (file && row.startsWith('+')) {
      line++;
    }
  }
  return findings;
}

function filesUnder(path) {
  if (!statSync(path).isDirectory()) return [path];
  return readdirSync(path)
    .filter((name) => !SKIPPED_DIRECTORIES.has(name))
    .sort()
    .flatMap((name) => filesUnder(join(path, name)));
}

function loadLiterals() {
  const path = process.env.PORT_SCAN_LITERALS_FILE;
  if (!path || !existsSync(path)) return null;
  const literals = readFileSync(path, 'utf8')
    .split('\n')
    .map((literal) => literal.trim())
    .filter((literal) => literal !== '');
  return literals.length === 0 ? null : literals;
}

function main(args) {
  const literals = loadLiterals();
  if (!literals) {
    console.error('PORT_SCAN_LITERALS_FILE must name an existing, non-empty file of private literals');
    return 2;
  }
  let findings;
  if (args[0] === '--diff') {
    if (!args[1]) {
      console.error('usage: scan.mjs --diff <base>');
      return 2;
    }
    try {
      findings = scanAddedLines(args[1], literals);
    } catch {
      console.error('git diff failed');
      return 2;
    }
  } else {
    const missing = args.filter((path) => !existsSync(path));
    if (args.length === 0 || missing.length > 0) {
      console.error(`path not found: ${missing.join(', ') || '(none given)'}`);
      return 2;
    }
    findings = args.flatMap(filesUnder).flatMap((file) => {
      const content = readFileSync(file);
      if (content.includes(0)) {
        console.error(`skipped binary ${file}`);
        return [];
      }
      return scanText(content.toString('utf8'), file, literals);
    });
  }
  findings.forEach((finding) => console.log(finding));
  return findings.length === 0 ? 0 : 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = main(process.argv.slice(2));
}
