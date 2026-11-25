const API_HOST = 'api.docudevs.ai';
const API_VARIABLES = ['$API_URL', '${API_URL}', '$DOCUDEVS_API_URL', '${DOCUDEVS_API_URL}'];
const BODY_FLAGS = new Set(['-d', '--data', '--data-raw', '--data-binary', '--json', '-F', '--form', '--form-string', '-T']);
const METHOD_FLAGS = new Set(['-X', '--request']);
const END = Symbol('end');

function tokenize(text) {
  let source = '';
  const lineAt = [];
  for (let i = 0, line = 1; i < text.length; i++) {
    const continuation = /^\\\r?\n/.exec(text.slice(i, i + 3));
    if (continuation) {
      source += ' ';
      lineAt.push(line);
      line++;
      i += continuation[0].length - 1;
    } else {
      source += text[i];
      lineAt.push(line);
      if (text[i] === '\n') line++;
    }
  }
  const tokens = [];
  const lines = [];
  let wordLine = 1;
  let word = null;
  let quote = null;
  const flush = () => {
    if (word !== null) {
      tokens.push(word);
      lines.push(wordLine);
    }
    word = null;
  };
  const pushEnd = (line) => {
    tokens.push(END);
    lines.push(line);
  };
  const begin = (index) => {
    if (word === null) {
      word = '';
      wordLine = lineAt[index];
    }
  };
  for (let i = 0; i < source.length; i++) {
    const char = source[i];
    if (quote) {
      if (char === quote) quote = null;
      else if (char === '\\' && quote === '"' && i + 1 < source.length) word += source[++i];
      else word += char;
    } else if (char === '"' || char === "'") {
      quote = char;
      begin(i);
    } else if (char === '\\' && i + 1 < source.length) {
      begin(i);
      word += source[++i];
    } else if (char === '#' && word === null) {
      while (i + 1 < source.length && source[i + 1] !== '\n') i++;
    } else if (/\s/.test(char)) {
      flush();
      if (char === '\n') pushEnd(lineAt[i]);
    } else if (char === '|' || char === ';' || char === '&' || char === ')') {
      flush();
      pushEnd(lineAt[i]);
    } else if (char === '(' && word !== null && word.endsWith('$')) {
      word = null;
      pushEnd(lineAt[i]);
    } else {
      begin(i);
      word += char;
    }
  }
  flush();
  return { tokens, lines };
}

const isApiUrl = (arg) =>
  arg.startsWith(`https://${API_HOST}`) ||
  arg.startsWith(`http://${API_HOST}`) ||
  API_VARIABLES.some((variable) => arg.startsWith(variable));

const isVariableReference = (arg) => /^\$(\{\w+\}|\w+)$/.test(arg);

const isUrlCandidate = (arg) => /^https?:\/\//.test(arg) || API_VARIABLES.some((variable) => arg.startsWith(variable));

function findUrl(args) {
  const url = args.find(isUrlCandidate);
  if (url !== undefined) return url;
  return args.find((arg, index) => isVariableReference(arg) && !(args[index - 1] ?? '').startsWith('-')) ?? null;
}

function pathOf(url) {
  const withoutHost = url
    .replace(/^https?:\/\/[^/]+/, '')
    .replace(/^\$\{(API_URL|DOCUDEVS_API_URL)\}/, '')
    .replace(/^\$(API_URL|DOCUDEVS_API_URL)/, '');
  const path = withoutHost.split(/[?#]/)[0].replace(/\/+$/, '');
  const segments = path.split('/').map((segment) => (/\$\{?\w+\}?/.test(segment) ? '{}' : segment));
  return segments.join('/') || '/';
}

function requestFrom(args, url) {
  if (url === null || !isApiUrl(url)) return null;
  let method = null;
  let hasBody = false;
  args.forEach((arg, index) => {
    if (METHOD_FLAGS.has(arg) && args[index + 1] !== undefined) method = args[index + 1].toUpperCase();
    if (BODY_FLAGS.has(arg)) hasBody = true;
  });
  return { method: method ?? (hasBody ? 'POST' : 'GET'), path: pathOf(url) };
}

/** @returns {{ line: number, url: string | null, request: { method: string, path: string } | null }[]} */
export function extractCurlCommands(source) {
  const { tokens, lines } = tokenize(source);
  const commands = [];
  for (let i = 0; i < tokens.length; i++) {
    if (tokens[i] !== 'curl') continue;
    const args = [];
    for (let j = i + 1; j < tokens.length && tokens[j] !== END; j++) args.push(tokens[j]);
    const url = findUrl(args);
    commands.push({ line: lines[i], url, request: requestFrom(args, url) });
  }
  return commands;
}

/** @returns {{ method: string, path: string }[]} */
export function extractCurlRequests(source) {
  return extractCurlCommands(source)
    .map((command) => command.request)
    .filter((request) => request !== null);
}

export function hostOf(url) {
  return /^https?:\/\/([^/?#:]+)/.exec(url)?.[1] ?? null;
}

export function urlIsAllowed(url, allowedHosts) {
  if (url === null) return false;
  if (isVariableReference(url)) return true;
  const host = hostOf(url);
  return host !== null && allowedHosts.includes(host);
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
