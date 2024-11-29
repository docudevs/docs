<script lang="ts">
	import { onMount } from 'svelte';
	import * as swaggerYaml from './docudevs.json';
	import SwaggerUI from 'swagger-ui';
	import 'swagger-ui/dist/swagger-ui.css';

	onMount(async () => {
		SwaggerUI({
			spec: swaggerYaml,
			dom_id: '#swagger-ui-container'
		});
	});
</script>

<svelte:head>
	<title>OpenAPI Specification</title>
</svelte:head>

<div id="swagger-ui-container" />
