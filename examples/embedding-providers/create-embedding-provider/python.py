import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # credential_id is the "id" of a shared model credential (see Model credentials).
    credential_id = 42

    response = await client.create_embedding_provider(
        name="Our OpenAI embeddings",
        provider_type="openai",
        deployment_name="text-embedding-3-small",
        dimensions=1536,
        similarity_metric="cosine",
        credential_id=credential_id,
    )
    if response.status_code not in (200, 201):
        raise SystemExit(f"Create failed: {response.status_code} {response.content.decode()}")

    mutation = json.loads(response.content)
    revision = mutation["provider"]["latestRevision"]
    print(f"Revision {revision['id']}, reindex required: {mutation['reindexRequired']}")
    # docs:end


asyncio.run(main())
