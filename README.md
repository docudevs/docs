# DocuDevs documentation

Source for [docs.docudevs.ai](https://docs.docudevs.ai), built with [Docusaurus](https://docusaurus.io/). Pushes to `main` publish the site.

## Prerequisites

Node.js 20 or newer.

## Commands

```bash
npm ci                    # install dependencies
npm start                 # local development server
npm run build             # production build into build/
npm run examples:test     # example tooling tests
npm run examples:manifest # validate examples, check page references, build the manifest
npm run examples:coverage # check missing languages against known-gaps.txt
npm run examples:curl     # check cURL examples against the spec, shell syntax of CLI examples
npm run sync-openapi-spec # copy the files the main repository owns (explicit, never part of build)
node tools/examples/sync-spec.mjs --check # list those files that differ from the main checkout
```

The Python and Java example checks (Python needs `uv`, Java needs a JDK 17 installed), the example conventions and the writing workflow are described in [AGENTS.md](AGENTS.md).
