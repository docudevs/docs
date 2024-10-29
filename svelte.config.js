//import adapter from '@sveltejs/adapter-auto';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';
import { markdoc } from 'svelte-markdoc-preprocess';
import azure from 'svelte-adapter-azure-swa';
import adapter from '@sveltejs/adapter-static';


function absoulute(file) {
	return join(dirname(fileURLToPath(import.meta.url)), file);
}

/** @type {import('@sveltejs/kit').Config} */
const config = {
	// Consult https://svelte.dev/docs/kit/integrations
	// for more information about preprocessors
	preprocess: [vitePreprocess(),
	markdoc({
						tags: absoulute('./src/lib/Tags.svelte'),
						nodes: absoulute('./src/lib/Nodes.svelte'),
						partials: absoulute('./src/partials'),
						layouts: {
								default: absoulute('./src/lib/layouts/Default.svelte'),
		            alternative: absoulute('./src/lib/layouts/Alternative.svelte'),
							}
	})],
	extensions: ['.markdoc', '.svelte'],
	kit: {
		// adapter-auto only supports some environments, see https://svelte.dev/docs/kit/adapter-auto for a list.
		// If your environment is not supported, or you settled on a specific environment, switch out the adapter.
		// See https://svelte.dev/docs/kit/adapters for more information about adapters.
		adapter: adapter({
			// default options are shown. On some platforms
			// these options are set automatically — see below
			pages: 'build',
			assets: 'build',
			fallback: undefined,
			precompress: false,
			strict: true
		})
	}
};

export default config;
