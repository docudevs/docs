import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { discoverExamples } from './lib/discover.mjs';
import { checkJavaPackage } from './lib/java-package.mjs';
import { assertReferencesResolve, buildManifest } from './lib/manifest.mjs';
import { findReferences } from './lib/references.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const output = join(root, 'src', 'generated', 'examples.json');

try {
  const examples = discoverExamples(join(root, 'examples'));
  for (const example of examples) {
    const java = example.files.find((file) => file.lang === 'java');
    const problem = java && checkJavaPackage(example.name, java.source);
    if (problem) throw new Error(problem);
  }
  const manifest = buildManifest(examples);
  assertReferencesResolve(findReferences(join(root, 'docs')), manifest);
  mkdirSync(dirname(output), { recursive: true });
  writeFileSync(output, `${JSON.stringify(manifest, null, 2)}\n`);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
