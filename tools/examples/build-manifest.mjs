import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from './lib/discover.mjs';
import { assertReferencesResolve, buildManifest } from './lib/manifest.mjs';
import { findReferences } from './lib/references.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const output = join(root, 'src', 'generated', 'examples.json');

try {
  const manifest = buildManifest(discoverExamples(join(root, 'examples')));
  assertReferencesResolve(findReferences(join(root, 'docs')), manifest);
  mkdirSync(dirname(output), { recursive: true });
  writeFileSync(output, `${JSON.stringify(manifest, null, 2)}\n`);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
