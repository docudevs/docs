const API_HOST = 'api.docudevs.ai';
const API_VARIABLES = ['$API_URL', '${API_URL}', '$DOCUDEVS_API_URL', '${DOCUDEVS_API_URL}'];
const BODY_FLAGS = new Set(['-d', '--data', '--data-raw', '--data-binary', '--json', '-F', '--form', '--form-string', '-T']);
const METHOD_FLAGS = new Set(['-X', '--request']);
const END = Symbol('end');

function tokenize(text) {
  const source = text.replace(/\\\r?\n/g, ' ');
  const tokens = [];
  let word = null;
  let quote = null;
  const flush = () => {
    if (word !== null) tokens.push(word);
    word = null;
  };
  for (let i = 0; i < source.length; i++) {
    const char = source[i];
    if (quote) {
      if (char === quote) quote = null;
      else if (char === '\\' && quote === '"' && i + 1 < source.length) word += source[++i];
      else word += char;
    } else if (char === '"' || char === "'") {
      quote = char;
      word ??= '';
    } else if (char === '\\' && i + 1 < source.length) {
      word = (word ?? '') + source[++i];
    } else if (char === '#' && word === null) {
      while (i + 1 < source.length && source[i + 1] !== '\n') i++;
    } else if (/\s/.test(char)) {
      flush();
      if (char === '\n') tokens.push(END);
    } else if (char === '|' || char === ';' || char === '&' || char === ')') {
      flush();
      tokens.push(END);
    } else if (char === '(' && word !== null && word.endsWith('$')) {
      word = null;
      tokens.push(END);
    } else {
      word = (word ?? '') + char;
    }
  }
  flush();
  return tokens;
}

const isApiUrl = (arg) =>
  arg.startsWith(`https://${API_HOST}`) ||
  arg.startsWith(`http://${API_HOST}`) ||
  API_VARIABLES.some((variable) => arg.startsWith(variable));

const isUrlCandidate = (arg) => /^https?:\/\//.test(arg) || API_VARIABLES.some((variable) => arg.startsWith(variable));

function pathOf(url) {
  const withoutHost = url
    .replace(/^https?:\/\/[^/]+/, '')
    .replace(/^\$\{(API_URL|DOCUDEVS_API_URL)\}/, '')
    .replace(/^\$(API_URL|DOCUDEVS_API_URL)/, '');
  const path = withoutHost.split(/[?#]/)[0].replace(/\/+$/, '');
  const segments = path.split('/').map((segment) => (/\$\{?\w+\}?/.test(segment) ? '{}' : segment));
  return segments.join('/') || '/';
}

function requestFrom(args) {
  const url = args.find(isUrlCandidate);
  if (url === undefined || !isApiUrl(url)) return null;
  let method = null;
  let hasBody = false;
  args.forEach((arg, index) => {
    if (METHOD_FLAGS.has(arg) && args[index + 1] !== undefined) method = args[index + 1].toUpperCase();
    if (BODY_FLAGS.has(arg)) hasBody = true;
  });
  return { method: method ?? (hasBody ? 'POST' : 'GET'), path: pathOf(url) };
}

/** @returns {{ method: string, path: string }[]} */
export function extractCurlRequests(source) {
  const tokens = tokenize(source);
  const requests = [];
  for (let i = 0; i < tokens.length; i++) {
    if (tokens[i] !== 'curl') continue;
    const args = [];
    for (let j = i + 1; j < tokens.length && tokens[j] !== END; j++) args.push(tokens[j]);
    const request = requestFrom(args);
    if (request) requests.push(request);
  }
  return requests;
}

const segmentsOf = (path) => path.split('/').filter((segment) => segment !== '');

const isParameter = (segment) => segment.startsWith('{') && segment.endsWith('}');

export function findOperation(spec, request) {
  const wanted = segmentsOf(request.path);
  const method = request.method.toLowerCase();
  return Object.entries(spec.paths ?? {}).some(([specPath, operations]) => {
    if (!Object.keys(operations).some((key) => key.toLowerCase() === method)) return false;
    const candidate = segmentsOf(specPath);
    return (
      candidate.length === wanted.length &&
      candidate.every((segment, index) => isParameter(segment) || wanted[index] === '{}' || segment === wanted[index])
    );
  });
}
