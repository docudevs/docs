import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { runFlatDocs } from './lib/flat-docs.mjs';

const root = process.env.FLAT_DOCS_ROOT ?? join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const { code, message } = runFlatDocs(root, { check: process.argv.includes('--check') });
(code === 0 ? console.log : console.error)(message);
process.exit(code);
