import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from './lib/discover.mjs';
import {
  DEFAULT_API_URL,
  executeRuns,
  formatReport,
  parseLiveArgs,
  parseOrderFile,
  parseSkipFile,
  redact,
  runExitCode,
  selectRuns,
} from './lib/live-run.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const outDir = join(root, 'live-run');
const venvBin = join(root, 'tools', 'examples', 'python', '.venv', 'bin');

const fail = (message) => {
  console.error(message);
  process.exit(2);
};

const apiKey = process.env.API_KEY ?? '';
if (apiKey === '') fail('API_KEY is not set: export the key of a test organization and run again.');

let options;
try {
  options = parseLiveArgs(process.argv.slice(2));
} catch (error) {
  fail(redact(error.message, apiKey));
}

let runs;
try {
  const examples = discoverExamples(join(root, 'examples'));
  const skip = parseSkipFile(
    readFileSync(join(root, 'examples', 'live-skip.txt'), 'utf8'),
    examples.map((example) => example.name),
  );
  const orderPath = join(root, 'examples', 'live-order.txt');
  const order = existsSync(orderPath)
    ? parseOrderFile(readFileSync(orderPath, 'utf8'), examples.map((example) => example.name))
    : [];
  runs = selectRuns({ examples, skip, only: options.only, variants: options.variants, order });
} catch (error) {
  fail(redact(error.message, apiKey));
}

if (options.list) {
  for (const run of runs) {
    console.log(`${run.name}\t${run.variant}\t${run.skip ? `skipped (${run.skip})` : 'run'}`);
  }
  console.log(`${runs.filter((run) => !run.skip).length} to run, ${runs.filter((run) => run.skip).length} skipped`);
  process.exit(0);
}

const needsPython = runs.some((run) => !run.skip && run.variant !== 'curl');
if (needsPython && !existsSync(join(venvBin, 'python'))) {
  fail('tools/examples/python/.venv is missing: see AGENTS.md (Checks) to create it, or run with --variant curl.');
}

rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

const results = await executeRuns({
  runs,
  examplesDir: join(root, 'examples'),
  fixturesDir: join(root, 'tools', 'examples', 'live-fixtures'),
  outDir,
  venvBin,
  apiKey,
  apiUrl: process.env.DOCUDEVS_API_URL || DEFAULT_API_URL,
});

const report = redact(formatReport(results), apiKey);
writeFileSync(join(outDir, 'report.md'), report);
console.log(report);
console.log('Report: live-run/report.md');
process.exit(runExitCode(results));
