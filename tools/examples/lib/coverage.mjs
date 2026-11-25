const GUARANTEED = ['curl', 'python', 'java'];

/** @returns {string[]} sorted "<name>:<lang>" entries */
export function computeGaps(examples) {
  return examples
    .flatMap((example) =>
      GUARANTEED.filter((lang) => !example.files.some((file) => file.lang === lang)).map(
        (lang) => `${example.name}:${lang}`,
      ),
    )
    .sort();
}

export function parseKnownGaps(text) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line !== '' && !line.startsWith('#'));
}

export function compareGaps(actual, known) {
  return {
    unlisted: actual.filter((gap) => !known.includes(gap)),
    stale: known.filter((gap) => !actual.includes(gap)),
  };
}
