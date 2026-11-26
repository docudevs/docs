const GUARANTEED = ['curl', 'python', 'java'];

/** @returns {string[]} sorted "<name>:<lang>" entries */
export function computeGaps(examples, notApplicable = []) {
  return examples
    .flatMap((example) =>
      GUARANTEED.filter((lang) => !example.files.some((file) => file.lang === lang))
        .map((lang) => `${example.name}:${lang}`)
        .filter((gap) => !notApplicable.includes(gap)),
    )
    .sort();
}

const NOT_APPLICABLE_LANGS = ['curl', 'python', 'java'];

export function parseNotApplicable(text) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line !== '' && !line.startsWith('#'))
    .map((line) => {
      const [pair, ...comment] = line.split('#');
      const entry = pair.trim();
      const lang = entry.slice(entry.lastIndexOf(':') + 1);
      if (!NOT_APPLICABLE_LANGS.includes(lang)) {
        throw new Error(`examples/not-applicable.txt: ${entry}: only curl, python and java may be listed, not ${lang}`);
      }
      if (comment.join('#').trim() === '') {
        throw new Error(`examples/not-applicable.txt: ${entry}: add a reason after '#'`);
      }
      return entry;
    });
}

export function staleNotApplicable(examples, notApplicable) {
  return notApplicable.filter((entry) => {
    const split = entry.lastIndexOf(':');
    const example = examples.find((candidate) => candidate.name === entry.slice(0, split));
    return !example || example.files.some((file) => file.lang === entry.slice(split + 1));
  });
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
