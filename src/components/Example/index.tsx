/// <reference types="@docusaurus/theme-classic" />

import type {ReactNode} from 'react';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import CodeBlock from '@theme/CodeBlock';
import examplesJson from '@site/src/generated/examples.json';

type Tab = {
  value: string;
  label: string;
  language: string;
  code: string;
};

type Manifest = Record<string, {tabs: Tab[]}>;

const manifest: Manifest = examplesJson;

export default function Example({name}: {name: string}): ReactNode {
  const example = manifest[name];
  if (!example) {
    throw new Error(`Unknown example "${name}"`);
  }
  return (
    <Tabs groupId="lang" queryString="lang">
      {example.tabs.map((tab) => (
        <TabItem key={tab.value} value={tab.value} label={tab.label}>
          <CodeBlock language={tab.language}>{tab.code}</CodeBlock>
        </TabItem>
      ))}
    </Tabs>
  );
}
