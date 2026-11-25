import { copyFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SPEC_TARGET = 'static/files/docudevs.yaml';
const SCHEMA_TARGET = 'docs/reference/pipeline.schema.json';
const SKILL_TARGET = 'static/files/playbook-evaluation-skill.md';

export function syncSpec({ mainRoot, destination }) {
  const source = join(mainRoot, 'docudevs.yaml');
  if (!existsSync(source)) return { copied: false };
  mkdirSync(dirname(destination), { recursive: true });
  copyFileSync(source, destination);
  const lines = readFileSync(destination, 'utf8').split('\n').length - 1;
  return { copied: true, lines };
}

function ownedFiles(mainRoot) {
  const files = [];
  const spec = join(mainRoot, 'docudevs.yaml');
  if (existsSync(spec)) {
    files.push({ target: SPEC_TARGET, expected: readFileSync(spec, 'utf8') });
  }
  const schema = join(mainRoot, 'pipeline.schema.json');
  if (existsSync(schema)) {
    files.push({ target: SCHEMA_TARGET, expected: readFileSync(schema, 'utf8') });
  }
  const skill = join(mainRoot, 'docudevs-sdk', 'skills', 'playbook-evaluation', 'SKILL.md');
  if (existsSync(skill)) {
    files.push({ target: SKILL_TARGET, expected: readFileSync(skill, 'utf8') });
  }
  return files;
}

function currentContent(docsRoot, target) {
  const path = join(docsRoot, target);
  return existsSync(path) ? readFileSync(path, 'utf8') : undefined;
}

export function syncOwnedFiles({ mainRoot, docsRoot }) {
  const copied = [];
  for (const { target, expected } of ownedFiles(mainRoot)) {
    const path = join(docsRoot, target);
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, expected);
    copied.push(target);
  }
  return { copied };
}

export function checkOwnedFiles({ mainRoot, docsRoot }) {
  return ownedFiles(mainRoot)
    .filter(({ target, expected }) => currentContent(docsRoot, target) !== expected)
    .map(({ target }) => target);
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
  const mainRoot = resolve(process.env.DOCUDEVS_MAIN_ROOT ?? join(root, '..', 'docudevs'));
  const found = existsSync(join(mainRoot, 'docudevs.yaml'));

  if (process.argv.includes('--check')) {
    if (!found) {
      console.log('No main checkout found; nothing to check');
    } else {
      const differing = checkOwnedFiles({ mainRoot, docsRoot: root });
      if (differing.length === 0) {
        console.log('Files owned by the main repository are in sync');
      } else {
        console.log(`Out of sync with the main repository:\n${differing.map((file) => `  ${file}`).join('\n')}`);
        process.exitCode = 1;
      }
    }
  } else if (!found) {
    console.log('No main checkout found; keeping vendored files');
  } else {
    const { copied } = syncOwnedFiles({ mainRoot, docsRoot: root });
    console.log(`Synced from the main repository:\n${copied.map((file) => `  ${file}`).join('\n')}`);
  }
}
