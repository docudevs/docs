<script lang="ts">
    export let title: string | null = null;
</script>

<svelte:head>
    <title>{title ? `${title} | ` : ''}DocuDevs</title>
    <meta
        name="description"
        content="DocuDevs is a platform for turning unstructured documents into structured data."
    />
</svelte:head>
<slot />
