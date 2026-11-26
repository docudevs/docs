import { spawn } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

export const ALLOWED_SKIP_REASONS = [
  'needs a signed-in user',
  'needs a dedicated deployment',
  'needs a public callback URL',
  'needs provider credentials',
  'deletes or changes organization-wide settings',
  'needs an existing resource id',
];

export const VARIANTS = ['curl', 'python', 'cli'];
const VARIANT_FILES = { curl: 'curl.sh', python: 'python.py', cli: 'cli.sh' };

export const DEFAULT_API_URL = 'https://api.docudevs.ai';
export const DEFAULT_TIMEOUT_MS = 10 * 60 * 1000;

export const OUTPUT_NAMES = [];

const EXTENSIONS = 'pdf|docx|xlsx|csv|png|jpg|jpeg|txt|json|md|zip';
const NAME = String.raw`[\w][\w.-]*\.(?:${EXTENSIONS})`;
const CANDIDATE = new RegExp(String.raw`(?<=^|[\s"'@(=])(${NAME})(?=$|[\s"'),\;|&])`, 'gim');
const WRITTEN_PATTERNS = [
  new RegExp(String.raw`open\(\s*["'](${NAME})["']\s*,\s*["'][wax]`, 'gi'),
  new RegExp(String.raw`(?:^|\s)(?:-o|--output|-O)\s+["']?(${NAME})`, 'gim'),
  new RegExp(String.raw`>>?\s*["']?(${NAME})`, 'gi'),
  new RegExp(String.raw`save_to\s*=\s*["'](${NAME})["']`, 'gi'),
  new RegExp(String.raw`Path\(\s*["'](${NAME})["']\s*\)\s*\.write_`, 'gi'),
];

const matches = (pattern, text) => Array.from(text.matchAll(pattern), (match) => match[1]);

export function detectFixtureReads(source, outputNames = OUTPUT_NAMES) {
  const written = new Set([...outputNames, ...WRITTEN_PATTERNS.flatMap((pattern) => matches(pattern, source))]);
  return [...new Set(matches(CANDIDATE, source))].filter((name) => !written.has(name));
}

export function parseSkipFile(text, knownNames) {
  const known = new Set(knownNames);
  const skip = new Map();
  text.split('\n').forEach((raw, index) => {
    const line = raw.replace(/\s+$/, '');
    if (line.trim() === '' || line.trim().startsWith('#')) return;
    const where = `examples/live-skip.txt:${index + 1}`;
    const tab = line.indexOf('\t');
    const name = (tab === -1 ? line : line.slice(0, tab)).trim();
    const reason = tab === -1 ? '' : line.slice(tab + 1).trim();
    if (!known.has(name)) throw new Error(`${where}: ${name} is not an example in the manifest`);
    if (reason === '') throw new Error(`${where}: ${name} needs a reason after a tab`);
    if (!ALLOWED_SKIP_REASONS.includes(reason)) {
      throw new Error(`${where}: "${reason}" is not an allowed reason (${ALLOWED_SKIP_REASONS.join('; ')})`);
    }
    if (skip.has(name)) throw new Error(`${where}: ${name} is listed twice`);
    skip.set(name, reason);
  });
  return skip;
}

export function parseLiveArgs(argv) {
  const options = { only: undefined, variants: [...VARIANTS], list: false };
  for (let index = 0; index < argv.length; index += 1) {
    const flag = argv[index];
    if (flag === '--list') {
      options.list = true;
    } else if (flag === '--only' || flag === '--variant') {
      const value = argv[index + 1];
      if (value === undefined || value.startsWith('--')) throw new Error(`${flag} needs a value`);
      index += 1;
      if (flag === '--only') {
        options.only = value;
      } else {
        if (!VARIANTS.includes(value)) throw new Error(`unknown variant "${value}"; use curl, python or cli`);
        options.variants = [value];
      }
    } else {
      throw new Error(`unknown flag ${flag}`);
    }
  }
  return options;
}

const matchesOnly = (name, only) => only === undefined || name === only || name.startsWith(`${only}/`);

export function selectRuns({ examples, skip, only, variants = VARIANTS }) {
  return examples
    .filter((example) => matchesOnly(example.name, only))
    .flatMap((example) =>
      VARIANTS.filter((variant) => variants.includes(variant) && example.files.some((file) => file.lang === variant)).map(
        (variant) => ({ name: example.name, variant, skip: skip.get(example.name) ?? null }),
      ),
    );
}

export function redact(text, key) {
  return key ? text.split(key).join('***') : text;
}

const lastNonEmptyLine = (text) => text.split('\n').map((line) => line.trimEnd()).filter((line) => line !== '').pop() ?? '';

function commandFor(variant, venvBin) {
  if (variant === 'python') return { command: join(venvBin, 'python'), args: [VARIANT_FILES.python] };
  return { command: 'bash', args: [VARIANT_FILES[variant]] };
}

function execute({ command, args, cwd, env, timeoutMs }) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { cwd, env, detached: true, stdio: ['ignore', 'pipe', 'pipe'] });
    const chunks = [];
    let timedOut = false;
    const timer = setTimeout(() => {
      timedOut = true;
      try {
        process.kill(-child.pid, 'SIGKILL');
      } catch {
        child.kill('SIGKILL');
      }
    }, timeoutMs);
    child.stdout.on('data', (chunk) => chunks.push(chunk));
    child.stderr.on('data', (chunk) => chunks.push(chunk));
    const finish = (exitCode, spawnError) => {
      clearTimeout(timer);
      const output = Buffer.concat(chunks).toString('utf8') + (spawnError ? `${spawnError}\n` : '');
      resolve({ exitCode, timedOut, output });
    };
    child.on('error', (error) => finish(null, error.message));
    child.on('close', (code) => finish(code));
  });
}

export async function executeRuns({
  runs,
  examplesDir,
  fixturesDir,
  outDir,
  venvBin,
  apiKey,
  apiUrl = DEFAULT_API_URL,
  parentEnv = process.env,
  timeoutMs = DEFAULT_TIMEOUT_MS,
  log = console.log,
}) {
  const results = [];
  for (const run of runs) {
    const base = { name: run.name, variant: run.variant };
    if (run.skip) {
      results.push({ ...base, status: 'skipped', reason: run.skip });
      continue;
    }
    const env = { API_KEY: apiKey, PATH: parentEnv.PATH ?? '', HOME: parentEnv.HOME ?? '', DOCUDEVS_API_URL: apiUrl };
    if (run.variant === 'cli') env.PATH = `${venvBin}:${env.PATH}`;
    const cwd = mkdtempSync(join(tmpdir(), 'docudevs-live-'));
    try {
      if (existsSync(fixturesDir)) cpSync(fixturesDir, cwd, { recursive: true });
      cpSync(join(examplesDir, run.name, VARIANT_FILES[run.variant]), join(cwd, VARIANT_FILES[run.variant]));
      const { command, args } = commandFor(run.variant, venvBin);
      const outcome = await execute({ command, args, cwd, env, timeoutMs });
      const output = redact(outcome.output, apiKey);
      const logDir = join(outDir, run.name);
      mkdirSync(logDir, { recursive: true });
      writeFileSync(join(logDir, `${run.variant}.log`), output);
      const passed = !outcome.timedOut && outcome.exitCode === 0;
      const result = passed
        ? { ...base, status: 'pass' }
        : {
            ...base,
            status: 'fail',
            exitCode: outcome.exitCode,
            timedOut: outcome.timedOut,
            lastLine: lastNonEmptyLine(output),
          };
      results.push(result);
      log(redact(`${run.name} ${run.variant}: ${describe(result)}`, apiKey));
    } finally {
      rmSync(cwd, { recursive: true, force: true });
    }
  }
  return results;
}

function describe(result) {
  if (result.status === 'pass') return 'pass';
  if (result.status === 'skipped') return `skipped (${result.reason})`;
  const how = result.timedOut ? 'timed out' : `exit ${result.exitCode}`;
  return `fail (${how})${result.lastLine ? `: ${result.lastLine}` : ''}`;
}

const cell = (text) => text.replace(/\|/g, '\\|').replace(/\s+/g, ' ');

export function formatReport(results) {
  const counts = (status) => results.filter((result) => result.status === status).length;
  const lines = [
    '# Live example run',
    '',
    `${counts('pass')} passed, ${counts('fail')} failed, ${counts('skipped')} skipped`,
    '',
    '| Example | Variant | Result |',
    '| --- | --- | --- |',
    ...results.map((result) => `| ${result.name} | ${result.variant} | ${cell(describe(result))} |`),
    '',
  ];
  return lines.join('\n');
}

export const runExitCode = (results) => (results.some((result) => result.status === 'fail') ? 1 : 0);
