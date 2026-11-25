import { LANGS } from './discover.mjs';

/**
 * @param {import('./discover.mjs').Example[]} examples
 * @returns {import('./discover.mjs').Manifest}
 */
export function buildManifest(examples) {
  return Object.fromEntries(
    examples.map((example) => [
      example.name,
      {
        tabs: example.files.map((file) => {
          const lang = LANGS.find((candidate) => candidate.value === file.lang);
          return { value: lang.value, label: lang.label, language: lang.language, code: file.snippet };
        }),
      },
    ]),
  );
}

export function assertReferencesResolve(refs, manifest) {
  const unknown = refs.filter((ref) => !Object.hasOwn(manifest, ref.name));
  if (unknown.length > 0) {
    const lines = unknown.map((ref) => `  ${ref.page}: unknown example "${ref.name}"`);
    throw new Error(`Unresolved <Example> references:\n${lines.join('\n')}`);
  }
}
