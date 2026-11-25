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
npm run examples:manifest # validate examples and build the manifest
npm run examples:coverage # check examples used by pages exist
npm run examples:curl     # check cURL examples against the spec
```

The Python and Java example checks, the example conventions and the writing workflow are described in [AGENTS.md](AGENTS.md).
