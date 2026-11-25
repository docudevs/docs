import { copyFileSync, existsSync, mkdirSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export function syncSpec({ mainRoot, destination }) {
  const source = join(mainRoot, 'docudevs.yaml');
  if (!existsSync(source)) return { copied: false };
  mkdirSync(dirname(destination), { recursive: true });
  copyFileSync(source, destination);
  const lines = readFileSync(destination, 'utf8').split('\n').length - 1;
  return { copied: true, lines };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
  const mainRoot = resolve(process.env.DOCUDEVS_MAIN_ROOT ?? join(root, '..', 'docudevs'));
  const result = syncSpec({ mainRoot, destination: join(root, 'static', 'files', 'docudevs.yaml') });
  console.log(
    result.copied
      ? `Synced docudevs.yaml (${result.lines} lines)`
      : 'No main checkout found; keeping vendored spec',
  );
}
