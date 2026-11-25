import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { compareGaps, computeGaps, parseKnownGaps, parseNotApplicable, staleNotApplicable } from './lib/coverage.mjs';
import { discoverExamples } from './lib/discover.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

try {
  const examples = discoverExamples(join(root, 'examples'));
  const notApplicable = parseNotApplicable(readFileSync(join(root, 'examples', 'not-applicable.txt'), 'utf8'));
  const actual = computeGaps(examples, notApplicable);
  const known = parseKnownGaps(readFileSync(join(root, 'examples', 'known-gaps.txt'), 'utf8'));
  const { unlisted, stale } = compareGaps(actual, known);
  const problems = [
    ...unlisted.map((gap) => `${gap}: add the example or list it in examples/known-gaps.txt`),
    ...stale.map((gap) => `${gap}: remove from examples/known-gaps.txt`),
    ...staleNotApplicable(examples, notApplicable).map(
      (entry) => `${entry}: the file exists or the example is gone; remove from examples/not-applicable.txt`,
    ),
  ];
  if (problems.length > 0) {
    console.error(problems.join('\n'));
    process.exit(1);
  }
  console.log(`Coverage OK: ${known.length} known gaps`);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
