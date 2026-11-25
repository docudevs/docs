import assert from 'node:assert/strict';
import { test } from 'node:test';
import { checkJavaPackage, expectedPackage } from '../lib/java-package.mjs';

test('expected package replaces dashes', () => {
  assert.equal(expectedPackage('contract-analysis/generate-profile'), 'examples.contract_analysis.generate_profile');
});

test('wrong package is reported', () => {
  assert.match(checkJavaPackage('cases/create', 'package examples.cases.list;\npublic class Example {}'), /examples\.cases\.create/);
});

test('matching package passes', () => {
  assert.equal(checkJavaPackage('cases/create', 'package examples.cases.create;\npublic class Example {}'), null);
});

test('missing package line is reported with example name and expected package', () => {
  assert.equal(
    checkJavaPackage('cases/create', 'public class Example {}'),
    'cases/create: Example.java must declare package examples.cases.create',
  );
});

test('missing public class is reported', () => {
  assert.equal(
    checkJavaPackage('cases/create', 'package examples.cases.create;\nclass Example {}'),
    'cases/create: Example.java must declare public class Example',
  );
});

test('package line after comments and with leading whitespace is accepted', () => {
  const source = '// header\n/* block\n   comment */\n  package examples.cases.create;\n\npublic class Example {}\n';
  assert.equal(checkJavaPackage('cases/create', source), null);
});

test('directory names that are not valid Java identifiers are rejected', () => {
  assert.throws(() => expectedPackage('cases/new'), /cases\/new/);
  assert.throws(() => expectedPackage('cases/2fa'), /cases\/2fa/);
});
