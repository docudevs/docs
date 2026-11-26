import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from './lib/discover.mjs';
import {
  DEFAULT_API_URL,
  executeRuns,
  formatReport,
  formatWritesBlock,
  parseLiveArgs,
  parseOrderFile,
  parseSkipFile,
  parseWritesFile,
  redact,
  runExitCode,
  selectRuns,
} from './lib/live-run.mjs';

const root = process.env.LIVE_RUN_ROOT || join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const outDir = join(root, 'live-run');
const venvBin = join(root, 'tools', 'examples', 'python', '.venv', 'bin');

const apiKey = process.env.API_KEY ?? '';

const fail = (message) => {
  console.error(redact(String(message), apiKey));
  process.exit(2);
};

const readOptional = (name) => {
  const path = join(root, 'examples', name);
  return existsSync(path) ? readFileSync(path, 'utf8') : '';
};

async function main() {
  if (apiKey === '') fail('API_KEY is not set: export the key of a test organization and run again.');

  const options = parseLiveArgs(process.argv.slice(2));
  const examples = discoverExamples(join(root, 'examples'));
  const names = examples.map((example) => example.name);
  const skip = parseSkipFile(readOptional('live-skip.txt'), names);
  const order = parseOrderFile(readOptional('live-order.txt'), names);
  const writes = parseWritesFile(readOptional('live-writes.txt'), names);
  const runs = selectRuns({ examples, skip, only: options.only, variants: options.variants, order });

  if (runs.length === 0) fail('No example matches the selection.');

  if (options.list) {
    for (const run of runs) {
      console.log(`${run.name}\t${run.variant}\t${run.skip ? `skipped (${run.skip})` : 'run'}`);
    }
    console.log(`${runs.filter((run) => !run.skip).length} to run, ${runs.filter((run) => run.skip).length} skipped`);
    return 0;
  }

  const runnable = [...new Set(runs.filter((run) => !run.skip).map((run) => run.name))];
  console.log(formatWritesBlock(writes, runnable));
  if (!options.yes) fail('re-run with --yes to run against this organization');

  const needsPython = runs.some((run) => !run.skip && run.variant !== 'curl');
  if (needsPython && !existsSync(join(venvBin, 'python'))) {
    fail('tools/examples/python/.venv is missing: see AGENTS.md (Checks) to create it, or run with --variant curl.');
  }

  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });

  const controller = new AbortController();
  const interrupt = () => controller.abort();
  process.on('SIGINT', interrupt);
  process.on('SIGTERM', interrupt);

  const results = await executeRuns({
    runs,
    examplesDir: join(root, 'examples'),
    fixturesDir: join(root, 'tools', 'examples', 'live-fixtures'),
    outDir,
    venvBin,
    apiKey,
    apiUrl: process.env.DOCUDEVS_API_URL || DEFAULT_API_URL,
    signal: controller.signal,
  });

  const report = redact(formatReport(results), apiKey);
  writeFileSync(join(outDir, 'report.md'), report);
  console.log(report);
  console.log('Report: live-run/report.md');
  if (controller.signal.aborted) return 130;
  return runExitCode(results);
}

try {
  process.exit(await main());
} catch (error) {
  fail(error.message);
}
