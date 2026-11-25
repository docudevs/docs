import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import yaml from 'js-yaml';
import { extractCurlCommands, findOperation, urlIsAllowed } from './lib/curl.mjs';
import { discoverExamples } from './lib/discover.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const ALLOWED_NON_API_HOSTS = [];
const SHELL_LANGS = new Set(['curl', 'cli']);

try {
  const spec = yaml.load(readFileSync(join(root, 'static', 'files', 'docudevs.yaml'), 'utf8'));
  const files = discoverExamples(join(root, 'examples')).flatMap((example) =>
    example.files.filter((file) => SHELL_LANGS.has(file.lang)),
  );
  const problems = [];
  let requestCount = 0;
  for (const file of files) {
    const syntax = spawnSync('bash', ['-n'], { input: file.source, encoding: 'utf8' });
    if (syntax.status !== 0) problems.push(`${file.file}: ${syntax.stderr.trim()}`);
    if (file.lang !== 'curl') continue;
    for (const { line, url, request } of extractCurlCommands(file.source)) {
      if (request === null) {
        if (!urlIsAllowed(url, ALLOWED_NON_API_HOSTS)) {
          problems.push(`${file.file}:${line}: curl to ${url ?? '(no URL)'} is not a DocuDevs API URL`);
        }
        continue;
      }
      requestCount++;
      if (!findOperation(spec, request)) {
        problems.push(`${file.file}:${line}: ${request.method} ${request.path} not in docudevs.yaml`);
      }
    }
  }
  if (problems.length > 0) {
    console.error(problems.join('\n'));
    process.exit(1);
  }
  console.log(`${files.length} files, ${requestCount} requests checked`);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
