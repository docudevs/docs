const MARKER = /(?:#|\/\/)\s*docs:(start|end)$/;

function markerKind(line) {
  const match = MARKER.exec(line.trim());
  return match ? match[1] : null;
}

function dedent(lines) {
  const indents = lines
    .filter((line) => line.trim() !== '')
    .map((line) => /^[ \t]*/.exec(line)[0].length);
  const common = indents.length ? Math.min(...indents) : 0;
  return lines.map((line) => line.slice(common));
}

function trimBlankEdges(lines) {
  let start = 0;
  let end = lines.length;
  while (start < end && lines[start].trim() === '') start++;
  while (end > start && lines[end - 1].trim() === '') end--;
  return lines.slice(start, end);
}

export function extractSnippet(source, file) {
  const lines = source.replace(/\r\n/g, '\n').split('\n');
  if (lines[lines.length - 1] === '') lines.pop();

  if (!lines.some((line) => markerKind(line))) {
    const body = lines[0]?.startsWith('#!') ? lines.slice(1) : lines;
    return trimBlankEdges(body).join('\n');
  }

  const regions = [];
  let current = null;
  for (const line of lines) {
    const kind = markerKind(line);
    if (kind === 'start') {
      if (current) throw new Error(`${file}: docs:start inside an open region, expected docs:end first`);
      current = [];
    } else if (kind === 'end') {
      if (!current) throw new Error(`${file}: docs:end without a preceding docs:start`);
      const region = trimBlankEdges(dedent(current)).join('\n');
      if (region !== '') regions.push(region);
      current = null;
    } else if (current) {
      current.push(line);
    }
  }
  if (current) throw new Error(`${file}: docs:start without a matching docs:end`);
  return regions.join('\n\n');
}
