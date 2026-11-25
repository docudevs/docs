const RESERVED_WORDS = new Set([
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
  'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
  'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native',
  'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
  'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void',
  'volatile', 'while', 'true', 'false', 'null', '_',
]);

/**
 * @param {string} exampleName "<area>/<name>"
 * @returns {string}
 */
export function expectedPackage(exampleName) {
  const segments = exampleName.split('/').map((segment) => segment.replaceAll('-', '_'));
  for (const segment of segments) {
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(segment) || RESERVED_WORDS.has(segment)) {
      throw new Error(`${exampleName}: "${segment}" is not a valid Java package segment`);
    }
  }
  return `examples.${segments.join('.')}`;
}

/**
 * @param {string} exampleName
 * @param {string} source contents of Example.java
 * @returns {string | null} an error message, or null when the file follows the convention
 */
export function checkJavaPackage(exampleName, source) {
  const expected = expectedPackage(exampleName);
  const declared = /^\s*package\s+([\w.]+)\s*;/m.exec(source);
  if (!declared || declared[1] !== expected) {
    return `${exampleName}: Example.java must declare package ${expected}`;
  }
  if (!/^\s*public\s+(?:final\s+)?class\s+Example\b/m.test(source)) {
    return `${exampleName}: Example.java must declare public class Example`;
  }
  return null;
}
